package com.gridweaver.gridweaver.model;

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
public class SolarPanel extends Device {
    private double generationPower; // Active solar generation in kW
    private double panelEfficiency; // Efficiency rating in % (e.g. 21.5%)
}
