package com.gridweaver.gridweaver.model;

import com.gridweaver.gridweaver.state.BatteryState;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Battery extends Device {
    private double batteryPercentage; // State of Charge (0.0 to 100.0%)
    private double capacity;          // Capacity in kWh (e.g. 13.5 kWh)
    private double temperature;       // Temperature in Celsius
    private BatteryState state;       // IDLE, CHARGING, DISCHARGING, FAULT

    @lombok.Builder.Default
    private double maxChargeRate = 5.0;    // max charge in kW

    @lombok.Builder.Default
    private double maxDischargeRate = 5.0; // max discharge in kW

    @lombok.Builder.Default
    private String health = "GOOD";        // "GOOD", "DEGRADED", "CRITICAL"
}
