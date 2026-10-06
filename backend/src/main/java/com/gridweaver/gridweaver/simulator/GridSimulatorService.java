package com.gridweaver.gridweaver.simulator;

import com.gridweaver.gridweaver.dto.DeviceTelemetryRequest;
import com.gridweaver.gridweaver.model.DeviceType;
import com.gridweaver.gridweaver.model.GridStatus;
import com.gridweaver.gridweaver.service.TelemetryService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class GridSimulatorService {

    private final TelemetryService telemetryService;
    private final com.gridweaver.gridweaver.service.DeviceRegistry deviceRegistry;

    @Value("${gridweaver.simulator.enabled:true}")
    private boolean enabled;

    @Value("${gridweaver.simulator.device-count:10}")
    private int initialDeviceCount;

    @Value("${gridweaver.simulator.interval-ms:1000}")
    private int intervalMs;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean stormMode = new AtomicBoolean(false);
    private final AtomicInteger activeTasks = new AtomicInteger(0);

    @Getter
    private volatile SimulationMode activeMode = SimulationMode.NORMAL;

    @Getter
    private volatile double currentGridLoad = 55.0; // Simulated control percentage (0-100%)

    private ExecutorService virtualThreadExecutor;
    private final ConcurrentHashMap<String, AtomicBoolean> deviceTaskControls = new ConcurrentHashMap<>();

    // Regional zone centroids spanning Northern California (~150km scale coverage for up to 100,000 nodes)
    private static final double[][] ZONE_CENTROIDS = {
        {37.950, -122.530}, // ZONE-1: North Bay / Marin Sector
        {37.775, -122.420}, // ZONE-2: Central San Francisco Sector
        {37.805, -122.250}, // ZONE-3: East Bay Sector (Oakland / Berkeley)
        {37.520, -122.280}, // ZONE-4: South Peninsula Sector (San Mateo / Redwood City)
        {37.335, -121.890}, // ZONE-5: Silicon Valley Sector (San Jose)
        {38.290, -122.450}, // ZONE-6: Sonoma & Napa Valley Solar Field Sector
        {37.900, -122.060}, // ZONE-7: Contra Costa & Tri-Valley Grid Sector
        {36.974, -122.030}  // ZONE-8: Santa Cruz & Coastal Microgrid Sector
    };

    @PostConstruct
    public void init() {
        if (enabled) {
            startSimulation(initialDeviceCount);
        }
    }

    @PreDestroy
    public void shutdown() {
        stopSimulation();
    }

    public synchronized void startSimulation(int deviceCount) {
        stopSimulation();
        deviceRegistry.clear();
        running.set(true);

        // Java 21 Virtual Thread Per Task Executor
        virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        log.info("Starting Java 21 Virtual Thread IoT Simulator with {} devices...", deviceCount);

        // Dynamically scale active zones based on device count
        int numZones = deviceCount <= 100 ? 4 : (deviceCount <= 1000 ? 6 : 8);

        // Dynamic geographic spread radius that scales smoothly with node count (10 -> 100 -> 1K -> 10K -> 100K)
        double spreadRadius = 0.03 + Math.log10(Math.max(10, deviceCount)) * 0.025;

        for (int i = 1; i <= deviceCount; i++) {
            boolean isBattery = (i % 2 != 0); // Alternate battery and solar
            String deviceId = isBattery
                    ? String.format("BATTERY-%05d", (i + 1) / 2)
                    : String.format("SOLAR-%05d", (i + 1) / 2);

            DeviceType type = isBattery ? DeviceType.BATTERY : DeviceType.SOLAR_PANEL;

            // Distribute hybrid pairs (Solar + Battery) evenly across all active zones
            int zoneIdx = ((i - 1) / 2) % numZones;
            String zoneId = "ZONE-" + (zoneIdx + 1);

            double baseLat = ZONE_CENTROIDS[zoneIdx][0];
            double baseLon = ZONE_CENTROIDS[zoneIdx][1];

            double lat, lon;
            if (type == DeviceType.SOLAR_PANEL) {
                // Solar arrays positioned in structured solar park clusters
                int subCluster = (i / 10) % 3;
                double offsetLat = (subCluster - 1) * (spreadRadius * 0.4);
                lat = baseLat + offsetLat + (ThreadLocalRandom.current().nextDouble() - 0.5) * spreadRadius;
                lon = baseLon + (ThreadLocalRandom.current().nextDouble() - 0.5) * (spreadRadius * 1.2);
            } else {
                // Battery storage spread across urban/industrial grids
                lat = baseLat + (ThreadLocalRandom.current().nextDouble() - 0.5) * spreadRadius;
                lon = baseLon + (ThreadLocalRandom.current().nextDouble() - 0.5) * spreadRadius;
            }

            AtomicBoolean taskRunning = new AtomicBoolean(true);
            deviceTaskControls.put(deviceId, taskRunning);

            activeTasks.incrementAndGet();
            virtualThreadExecutor.submit(() -> runDeviceSimulation(deviceId, type, lat, lon, zoneId, taskRunning));
        }

        // Background virtual thread to update simulated gridLoad fluctuation
        virtualThreadExecutor.submit(this::runGridLoadFluctuationLoop);
    }

    public synchronized void stopSimulation() {
        running.set(false);
        deviceTaskControls.values().forEach(flag -> flag.set(false));
        deviceTaskControls.clear();
        activeTasks.set(0);

        if (virtualThreadExecutor != null) {
            virtualThreadExecutor.shutdownNow();
            virtualThreadExecutor = null;
        }
        log.info("Java 21 Virtual Thread IoT Simulator stopped.");
    }

    public void setMode(SimulationMode mode) {
        this.activeMode = mode != null ? mode : SimulationMode.NORMAL;
        this.stormMode.set(this.activeMode == SimulationMode.STORM);
        switch (this.activeMode) {
            case NORMAL -> {
                this.currentGridLoad = 55.0;
                log.info("Simulation mode: NORMAL (grid load ~55%)");
            }
            case HIGH_DEMAND -> {
                this.currentGridLoad = 88.0;
                log.info("Simulation mode: HIGH_DEMAND (grid load ~88%)");
            }
            case STORM -> {
                this.currentGridLoad = 92.0;
                log.warn("Simulation mode: STORM (grid load ~92%, heavy clouds, battery dispatch)");
            }
            case SOLAR_LOSS -> {
                this.currentGridLoad = 68.0;
                log.info("Simulation mode: SOLAR_LOSS (solar generation ~0 kW)");
            }
            case RECOVERY -> {
                this.currentGridLoad = 35.0;
                log.info("Simulation mode: RECOVERY (grid load ~35%, solar active, recharging)");
            }
        }
    }

    public void setStorm(boolean storm) {
        setMode(storm ? SimulationMode.STORM : SimulationMode.NORMAL);
    }

    public boolean isStormActive() {
        return stormMode.get();
    }

    public int getActiveTaskCount() {
        return running.get() ? deviceTaskControls.size() : 0;
    }

    public GridStatus getGridStatus() {
        return GridStatus.builder()
                .gridLoad(currentGridLoad)
                .stormActive(stormMode.get())
                .simulationMode(activeMode)
                .lastUpdated(Instant.now())
                .build();
    }

    private void runDeviceSimulation(String deviceId, DeviceType type, double lat, double lon, String zoneId, AtomicBoolean taskRunning) {
        double batterySoc = 70.0 + ThreadLocalRandom.current().nextDouble() * 25.0; // 70-95%
        double temp = 25.0 + ThreadLocalRandom.current().nextDouble() * 5.0;

        while (running.get() && taskRunning.get()) {
            try {
                double voltage = 228.0 + ThreadLocalRandom.current().nextDouble() * 4.0;
                double power;
                Double reqSoc = null;
                Double reqTemp = null;

                if (type == DeviceType.BATTERY) {
                    // Thermal dynamics in Storm Mode (causes representative battery units to exceed 50°C thermal limit)
                    int idNum = 1;
                    try {
                        idNum = Integer.parseInt(deviceId.replace("BATTERY-", ""));
                    } catch (Exception ignored) {}

                    boolean isStormThermalTarget = activeMode == SimulationMode.STORM && (idNum <= 5 || idNum % 20 == 1);

                    if (isStormThermalTarget) {
                        // Over-temperature thermal stress in storm mode -> exceeds 50°C safety threshold
                        temp = Math.min(55.0, temp + 1.2);
                    } else if (activeMode == SimulationMode.RECOVERY || activeMode == SimulationMode.NORMAL) {
                        temp = Math.max(25.0, temp - 0.8);
                    } else {
                        temp = Math.max(20.0, Math.min(42.0, temp + (ThreadLocalRandom.current().nextDouble() - 0.5)));
                    }
                    reqTemp = temp;

                    // Battery dispatch power based on active scenario, grid load, and thermal status
                    if (temp > 50.0) {
                        // POWER CUT / FAULT ISOLATION: Over-temperature emergency trip stops all power dispatch
                        power = 0.0;
                    } else if (batterySoc <= 5.0) {
                        // LOW BATTERY DEPLETION PROTECTION: Depleted battery stops discharging
                        power = 0.0;
                    } else if (currentGridLoad > 80.0) {
                        // High load -> discharging to support grid
                        power = 3.5 + ThreadLocalRandom.current().nextDouble() * 1.5;
                        batterySoc = Math.max(0.0, batterySoc - 0.08); // gradual drain
                    } else if (currentGridLoad < 40.0) {
                        // Low load (Recovery / off-peak) -> charging from grid
                        power = 2.5 + ThreadLocalRandom.current().nextDouble() * 1.5;
                        batterySoc = Math.min(100.0, batterySoc + 0.12);
                    } else {
                        power = 0.2 + ThreadLocalRandom.current().nextDouble() * 0.3; // idle baseline
                    }
                    reqSoc = batterySoc;

                } else {
                    // Solar Panel generation based on scenario
                    power = switch (activeMode) {
                        case STORM -> 0.2 + ThreadLocalRandom.current().nextDouble() * 0.3; // dense storm clouds
                        case SOLAR_LOSS -> 0.0; // complete solar disconnection / night
                        case RECOVERY -> 5.0 + ThreadLocalRandom.current().nextDouble() * 1.5; // clear sky peak
                        case HIGH_DEMAND -> 4.0 + ThreadLocalRandom.current().nextDouble() * 1.5;
                        case NORMAL -> 3.5 + ThreadLocalRandom.current().nextDouble() * 1.5;
                    };
                }

                double current = (voltage > 0) ? (power * 1000.0 / voltage) : 0.0;

                DeviceTelemetryRequest telemetry = DeviceTelemetryRequest.builder()
                        .deviceId(deviceId)
                        .deviceType(type)
                        .timestamp(Instant.now())
                        .latitude(lat)
                        .longitude(lon)
                        .zoneId(zoneId)
                        .power(power)
                        .voltage(voltage)
                        .current(current)
                        .batteryPercentage(reqSoc)
                        .temperature(reqTemp)
                        .gridLoad(currentGridLoad)
                        .build();

                telemetryService.processTelemetry(telemetry);

                // Non-blocking sleep on Java 21 Virtual Thread
                Thread.sleep(intervalMs);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error in virtual-thread telemetry loop for device {}: {}", deviceId, e.getMessage());
            }
        }
        activeTasks.decrementAndGet();
    }

    private void runGridLoadFluctuationLoop() {
        while (running.get()) {
            try {
                switch (activeMode) {
                    case STORM -> {
                        double delta = (ThreadLocalRandom.current().nextDouble() - 0.5) * 1.5;
                        currentGridLoad = Math.max(86.0, Math.min(96.0, currentGridLoad + delta));
                    }
                    case HIGH_DEMAND -> {
                        double delta = (ThreadLocalRandom.current().nextDouble() - 0.5) * 1.2;
                        currentGridLoad = Math.max(84.0, Math.min(94.0, currentGridLoad + delta));
                    }
                    case SOLAR_LOSS -> {
                        double delta = (ThreadLocalRandom.current().nextDouble() - 0.5) * 1.0;
                        currentGridLoad = Math.max(62.0, Math.min(76.0, currentGridLoad + delta));
                    }
                    case RECOVERY -> {
                        double delta = (ThreadLocalRandom.current().nextDouble() - 0.5) * 1.0;
                        currentGridLoad = Math.max(28.0, Math.min(38.0, currentGridLoad + delta));
                    }
                    case NORMAL -> {
                        double delta = (ThreadLocalRandom.current().nextDouble() - 0.5) * 2.0;
                        currentGridLoad = Math.max(45.0, Math.min(65.0, currentGridLoad + delta));
                    }
                }
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
