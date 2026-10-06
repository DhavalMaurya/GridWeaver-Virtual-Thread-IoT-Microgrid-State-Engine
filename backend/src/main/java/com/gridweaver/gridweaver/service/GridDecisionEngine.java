package com.gridweaver.gridweaver.service;

import com.gridweaver.gridweaver.model.AlertSeverity;
import com.gridweaver.gridweaver.model.AlertType;
import com.gridweaver.gridweaver.model.Battery;
import com.gridweaver.gridweaver.state.BatteryEvent;
import com.gridweaver.gridweaver.state.BatteryState;
import com.gridweaver.gridweaver.state.BatteryStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class GridDecisionEngine {

    private final BatteryStateService batteryStateService;
    private final AlertService alertService;

    @Value("${gridweaver.grid.discharge-threshold:80.0}")
    private double dischargeThreshold;

    @Value("${gridweaver.grid.charge-threshold:40.0}")
    private double chargeThreshold;

    @Value("${gridweaver.grid.temp-fault-threshold:50.0}")
    private double tempFaultThreshold;

    public void evaluate(Battery battery, Double gridLoad) {
        if (battery == null || battery.getId() == null) return;

        double load = gridLoad != null ? gridLoad : 50.0;
        BatteryState currentState = battery.getState() != null ? battery.getState() : BatteryState.IDLE;
        double temp = battery.getTemperature();
        double soc = battery.getBatteryPercentage();
        String id = battery.getId();

        // 1. PRIORITY 1: TEMPERATURE PROTECTION (> 50°C forces FAULT regardless of health or SoC)
        if (currentState == BatteryState.FAULT) {
            if (temp <= tempFaultThreshold) {
                batteryStateService.fireEvent(id, BatteryEvent.FAULT_CLEARED,
                        "Temperature normalized to " + String.format(Locale.US, "%.1f", temp) + "°C (<= " + tempFaultThreshold + "°C)", load);
                alertService.resolveActiveAlert(id, AlertType.HIGH_TEMPERATURE);
                alertService.resolveActiveAlert(id, AlertType.BATTERY_FAULT);
            }
            return; // In FAULT state, no other actions permitted until cleared
        }

        if (temp > tempFaultThreshold) {
            alertService.createAlert(
                    id,
                    AlertType.HIGH_TEMPERATURE,
                    AlertSeverity.CRITICAL,
                    "High temperature warning: " + String.format(Locale.US, "%.1f", temp) + "°C exceeded safety limit " + tempFaultThreshold + "°C"
            );
            batteryStateService.fireEvent(id, BatteryEvent.FAULT_DETECTED,
                    "High temperature: " + String.format(Locale.US, "%.1f", temp) + "°C (> " + tempFaultThreshold + "°C)", load);
            return;
        }

        // 2. PRIORITY 2: HEALTH CHECK (degraded or critical battery cannot operate)
        String health = battery.getHealth();
        if (health != null && !health.equalsIgnoreCase("GOOD")) {
            if (currentState != BatteryState.IDLE) {
                batteryStateService.fireEvent(id, BatteryEvent.STOP, "Battery health degraded (" + health + ")", load);
            }
            return;
        }

        // 3. PRIORITY 3: SoC BOUNDARIES (Low SoC <= 5% or High SoC >= 90%)
        if (soc <= 5.0) {
            alertService.createAlert(
                    id,
                    AlertType.LOW_BATTERY,
                    AlertSeverity.WARNING,
                    "Battery depleted: " + String.format(Locale.US, "%.1f", soc) + "% <= 5%"
            );
            if (currentState == BatteryState.DISCHARGING) {
                batteryStateService.fireEvent(id, BatteryEvent.STOP,
                        "Low battery protection: " + String.format(Locale.US, "%.1f", soc) + "% (SoC <= 5%)", load);
            }
            // Cannot discharge if SoC <= 5%
            if (load < chargeThreshold) {
                if (currentState != BatteryState.CHARGING) {
                    batteryStateService.fireEvent(id, BatteryEvent.START_CHARGING,
                            "Grid load low (" + String.format(Locale.US, "%.1f", load) + "%); charging depleted battery", load);
                }
            }
            return;
        } else if (soc > 15.0) {
            alertService.resolveActiveAlert(id, AlertType.LOW_BATTERY);
        }

        if (soc >= 90.0) {
            if (currentState == BatteryState.CHARGING) {
                batteryStateService.fireEvent(id, BatteryEvent.STOP,
                        "Full battery charge ceiling reached: " + String.format(Locale.US, "%.1f", soc) + "% (SoC >= 90%)", load);
            }
            // Cannot charge further if SoC >= 90%
            if (load > dischargeThreshold) {
                if (currentState != BatteryState.DISCHARGING) {
                    batteryStateService.fireEvent(id, BatteryEvent.START_DISCHARGING,
                            "Grid load " + String.format(Locale.US, "%.1f", load) + "% exceeded " + dischargeThreshold + "% threshold", load);
                }
            }
            return;
        }

        // 4. PRIORITY 4: DEMAND MATCHING (Dynamic Dispatch)
        if (load > dischargeThreshold) {
            if (currentState != BatteryState.DISCHARGING) {
                batteryStateService.fireEvent(id, BatteryEvent.START_DISCHARGING,
                        "Grid load " + String.format(Locale.US, "%.1f", load) + "% exceeded " + dischargeThreshold + "% threshold", load);
            }
            return;
        }

        if (load < chargeThreshold) {
            if (currentState != BatteryState.CHARGING) {
                batteryStateService.fireEvent(id, BatteryEvent.START_CHARGING,
                        "Grid load " + String.format(Locale.US, "%.1f", load) + "% below " + chargeThreshold + "% threshold", load);
            }
            return;
        }

        // Normal balanced grid load (40% - 80%) -> IDLE
        if (currentState != BatteryState.IDLE) {
            batteryStateService.fireEvent(id, BatteryEvent.STOP,
                    "Grid load balanced (" + String.format(Locale.US, "%.1f", load) + "%)", load);
        }
    }
}
