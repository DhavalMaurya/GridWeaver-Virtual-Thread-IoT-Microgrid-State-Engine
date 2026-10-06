package com.gridweaver.gridweaver.service;

import com.gridweaver.gridweaver.model.Battery;
import com.gridweaver.gridweaver.model.EnergyFlow;
import com.gridweaver.gridweaver.model.SolarPanel;
import com.gridweaver.gridweaver.simulator.GridSimulatorService;
import com.gridweaver.gridweaver.state.BatteryState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class EnergyFlowService {

    private final DeviceRegistry deviceRegistry;
    private final GridSimulatorService simulatorService;

    public EnergyFlow calculateCurrentFlow() {
        double solarGeneration = 0.0;
        double batteryCharging = 0.0;
        double batteryDischarging = 0.0;

        for (SolarPanel solar : deviceRegistry.findAllSolarPanels()) {
            if (solar.isOnline() && solar.isEnabled()) {
                solarGeneration += Math.max(0.0, solar.getPower());
            }
        }

        for (Battery battery : deviceRegistry.findAllBatteries()) {
            if (battery.isOnline() && battery.isEnabled()) {
                if (battery.getState() == BatteryState.CHARGING) {
                    batteryCharging += Math.abs(battery.getPower());
                } else if (battery.getState() == BatteryState.DISCHARGING) {
                    batteryDischarging += Math.abs(battery.getPower());
                }
            }
        }

        // gridConsumption is a simulated/derived control value for the MVP based on current gridLoad
        double gridLoadPercent = simulatorService.getCurrentGridLoad();
        int totalDevices = Math.max(1, deviceRegistry.count());
        double basePerDevice = 2.5; // kW average load per node
        double gridConsumption = (gridLoadPercent / 100.0) * totalDevices * basePerDevice;

        // netPowerFlow = solarGeneration + batteryDischarging - batteryCharging - gridConsumption
        double netPowerFlow = solarGeneration + batteryDischarging - batteryCharging - gridConsumption;

        return EnergyFlow.builder()
                .solarGeneration(solarGeneration)
                .batteryCharging(batteryCharging)
                .batteryDischarging(batteryDischarging)
                .gridConsumption(gridConsumption)
                .netPowerFlow(netPowerFlow)
                .timestamp(Instant.now())
                .build();
    }
}
