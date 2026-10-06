package com.gridweaver.gridweaver.service;

import com.gridweaver.gridweaver.model.Battery;
import com.gridweaver.gridweaver.model.SystemMetrics;
import com.gridweaver.gridweaver.simulator.GridSimulatorService;
import com.gridweaver.gridweaver.state.BatteryState;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@EnableScheduling
@RequiredArgsConstructor
public class MetricsService {

    private final DeviceRegistry deviceRegistry;
    private final GridSimulatorService simulatorService;

    @Scheduled(fixedRate = 1000)
    public void calculateRates() {
        deviceRegistry.calculateRates();
    }

    public SystemMetrics getSystemMetrics() {
        List<Battery> batteries = deviceRegistry.findAllBatteries();

        int idle = 0;
        int charging = 0;
        int discharging = 0;
        int fault = 0;

        for (Battery b : batteries) {
            if (b.getState() == BatteryState.CHARGING) charging++;
            else if (b.getState() == BatteryState.DISCHARGING) discharging++;
            else if (b.getState() == BatteryState.FAULT) fault++;
            else idle++;
        }

        return SystemMetrics.builder()
                .totalDevices(deviceRegistry.count())
                .onlineDevices(deviceRegistry.onlineCount())
                .telemetryPerSec(deviceRegistry.getTelemetryRate())
                .stateChangesPerSec(deviceRegistry.getStateChangeRate())
                .gridLoad(simulatorService.getCurrentGridLoad())
                .activeSimulatorTasks(simulatorService.getActiveTaskCount())
                .idleCount(idle)
                .chargingCount(charging)
                .dischargingCount(discharging)
                .faultCount(fault)
                .timestamp(Instant.now())
                .build();
    }
}
