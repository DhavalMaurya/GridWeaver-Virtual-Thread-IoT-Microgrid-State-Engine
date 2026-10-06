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
public class GridStatus {
    private double gridLoad;               // Simulated control percentage (0.0 to 100.0%)
    private double totalSolarGeneration;   // Total solar output across grid (kW)
    private double totalBatteryDischarge;  // Total battery discharge feeding grid (kW)
    private double totalBatteryCharge;     // Total battery absorption from grid (kW)
    private boolean stormActive;           // Whether storm stress simulation is running
    private com.gridweaver.gridweaver.simulator.SimulationMode simulationMode;
    private Instant lastUpdated;
}
