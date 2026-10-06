package com.gridweaver.gridweaver.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnergyFlow {
    private double solarGeneration;    // total solar generation in kW
    private double batteryCharging;     // total power flowing INTO batteries in kW
    private double batteryDischarging;  // total power flowing OUT OF batteries in kW
    private double gridConsumption;     // simulated/derived microgrid load in kW
    private double netPowerFlow;        // solar + discharge - consumption - charge
    private Instant timestamp;
}
