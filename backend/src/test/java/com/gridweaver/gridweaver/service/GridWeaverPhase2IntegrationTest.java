package com.gridweaver.gridweaver.service;

import com.gridweaver.gridweaver.dto.DeviceCreateRequest;
import com.gridweaver.gridweaver.dto.DeviceResponse;
import com.gridweaver.gridweaver.dto.DeviceTelemetryRequest;
import com.gridweaver.gridweaver.model.*;
import com.gridweaver.gridweaver.simulator.GridSimulatorService;
import com.gridweaver.gridweaver.simulator.SimulationMode;
import com.gridweaver.gridweaver.state.BatteryState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class GridWeaverPhase2IntegrationTest {

    @Autowired
    private DeviceRegistry deviceRegistry;

    @Autowired
    private TelemetryService telemetryService;

    @Autowired
    private GridDecisionEngine gridDecisionEngine;

    @Autowired
    private AlertService alertService;

    @Autowired
    private EnergyFlowService energyFlowService;

    @Autowired
    private GridSimulatorService simulatorService;

    @BeforeEach
    void setUp() {
        deviceRegistry.clear();
    }

    @Test
    void testDeviceManagementAndDynamicZones() {
        // Register devices in different zones
        Battery bat1 = Battery.builder()
                .id("BAT-ZONE-A")
                .deviceType(DeviceType.BATTERY)
                .zoneId("ZONE-NORTH")
                .online(true)
                .status(DeviceStatus.ONLINE)
                .batteryPercentage(80.0)
                .temperature(25.0)
                .health("GOOD")
                .build();

        SolarPanel sol1 = SolarPanel.builder()
                .id("SOLAR-ZONE-B")
                .deviceType(DeviceType.SOLAR_PANEL)
                .zoneId("ZONE-SOUTH")
                .online(true)
                .status(DeviceStatus.ONLINE)
                .generationPower(4.5)
                .build();

        deviceRegistry.save(bat1);
        deviceRegistry.save(sol1);

        // Dynamic zones extraction
        List<String> zones = deviceRegistry.getAvailableZones();
        assertEquals(2, zones.size());
        assertTrue(zones.contains("ZONE-NORTH"));
        assertTrue(zones.contains("ZONE-SOUTH"));

        // Filtering
        List<Device> batteryFilter = deviceRegistry.filterDevices(DeviceType.BATTERY, "ALL", null, null);
        assertEquals(1, batteryFilter.size());
        assertEquals("BAT-ZONE-A", batteryFilter.get(0).getId());

        List<Device> zoneFilter = deviceRegistry.filterDevices(null, "ZONE-SOUTH", null, null);
        assertEquals(1, zoneFilter.size());
        assertEquals("SOLAR-ZONE-B", zoneFilter.get(0).getId());

        // Deletion
        assertTrue(deviceRegistry.deleteById("BAT-ZONE-A"));
        assertFalse(deviceRegistry.findById("BAT-ZONE-A").isPresent());
    }

    @Test
    void testHeartbeatTimeoutDetection() {
        Battery oldDevice = Battery.builder()
                .id("BAT-STALE")
                .deviceType(DeviceType.BATTERY)
                .online(true)
                .status(DeviceStatus.ONLINE)
                .lastSeen(Instant.now().minusSeconds(15)) // 15 seconds stale
                .build();

        deviceRegistry.save(oldDevice);

        List<Device> timedOut = deviceRegistry.checkDeviceHeartbeats(5);
        assertEquals(1, timedOut.size());
        assertEquals("BAT-STALE", timedOut.get(0).getId());

        Device updated = deviceRegistry.findById("BAT-STALE").orElseThrow();
        assertFalse(updated.isOnline());
        assertEquals(DeviceStatus.OFFLINE, updated.getStatus());
    }

    @Test
    void testSmartBatteryPriorityChain() {
        String id = "BAT-PRIORITY-01";
        Battery battery = Battery.builder()
                .id(id)
                .deviceType(DeviceType.BATTERY)
                .online(true)
                .state(BatteryState.IDLE)
                .batteryPercentage(4.0) // SoC <= 5%
                .temperature(54.0)       // Temp > 50°C (CRITICAL)
                .health("DEGRADED")      // Health != GOOD
                .build();

        deviceRegistry.save(battery);

        // PRIORITY 1: Temperature Protection (> 50°C) MUST force FAULT
        // even though health is DEGRADED and SoC is depleted
        gridDecisionEngine.evaluate(battery, 90.0);
        assertEquals(BatteryState.FAULT, battery.getState(), "Over-temperature (> 50°C) must trigger FAULT immediately");

        // Verify High Temperature alert was raised
        List<Alert> alerts = alertService.getActiveAlerts();
        assertTrue(alerts.stream().anyMatch(a -> a.getDeviceId().equals(id) && a.getType() == AlertType.HIGH_TEMPERATURE));

        // Cool temperature down to 40°C -> FAULT clears back to IDLE
        battery.setTemperature(40.0);
        gridDecisionEngine.evaluate(battery, 50.0);
        assertEquals(BatteryState.IDLE, battery.getState(), "Cooling <= 50°C must clear FAULT");

        // PRIORITY 2: Health Check (health is DEGRADED) -> Must stay IDLE even under 90% grid load
        gridDecisionEngine.evaluate(battery, 90.0);
        assertEquals(BatteryState.IDLE, battery.getState(), "Degraded health must inhibit dispatch");

        // Now restore health to GOOD
        battery.setHealth("GOOD");

        // PRIORITY 3: SoC Boundaries (SoC = 4% <= 5%) -> Must NOT discharge under 90% load
        gridDecisionEngine.evaluate(battery, 90.0);
        assertNotEquals(BatteryState.DISCHARGING, battery.getState(), "Depleted battery (SoC <= 5%) must not discharge");

        // Restore SoC to 75% -> Under 90% load, PRIORITY 4 Demand Matching triggers DISCHARGING
        battery.setBatteryPercentage(75.0);
        gridDecisionEngine.evaluate(battery, 90.0);
        assertEquals(BatteryState.DISCHARGING, battery.getState(), "Healthy battery within SoC bounds must discharge on demand");
    }

    @Test
    void testEnergyFlowCalculation() {
        SolarPanel solar = SolarPanel.builder()
                .id("SOLAR-01")
                .deviceType(DeviceType.SOLAR_PANEL)
                .online(true)
                .enabled(true)
                .power(5.0)
                .build();

        Battery battery = Battery.builder()
                .id("BAT-01")
                .deviceType(DeviceType.BATTERY)
                .online(true)
                .enabled(true)
                .state(BatteryState.DISCHARGING)
                .power(3.5)
                .build();

        deviceRegistry.save(solar);
        deviceRegistry.save(battery);

        EnergyFlow flow = energyFlowService.calculateCurrentFlow();
        assertNotNull(flow);
        assertEquals(5.0, flow.getSolarGeneration(), 0.1);
        assertEquals(3.5, flow.getBatteryDischarging(), 0.1);
        assertEquals(0.0, flow.getBatteryCharging(), 0.1);
        assertTrue(flow.getGridConsumption() > 0, "Grid consumption should be derived from load");
    }

    @Test
    void testSimulationModes() {
        simulatorService.setMode(SimulationMode.NORMAL);
        assertEquals(SimulationMode.NORMAL, simulatorService.getActiveMode());
        assertEquals(55.0, simulatorService.getCurrentGridLoad(), 1.0);

        simulatorService.setMode(SimulationMode.HIGH_DEMAND);
        assertEquals(SimulationMode.HIGH_DEMAND, simulatorService.getActiveMode());
        assertEquals(88.0, simulatorService.getCurrentGridLoad(), 1.0);

        simulatorService.setMode(SimulationMode.STORM);
        assertEquals(SimulationMode.STORM, simulatorService.getActiveMode());
        assertTrue(simulatorService.isStormActive());

        simulatorService.setMode(SimulationMode.SOLAR_LOSS);
        assertEquals(SimulationMode.SOLAR_LOSS, simulatorService.getActiveMode());

        simulatorService.setMode(SimulationMode.RECOVERY);
        assertEquals(SimulationMode.RECOVERY, simulatorService.getActiveMode());
        assertEquals(35.0, simulatorService.getCurrentGridLoad(), 1.0);

        // Reset to normal
        simulatorService.setMode(SimulationMode.NORMAL);
    }
}
