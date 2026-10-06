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
public class SystemMetrics {
    private int totalDevices;
    private int onlineDevices;
    private long telemetryPerSec;
    private long stateChangesPerSec;
    private double gridLoad;            // Simulated control value %
    private int activeSimulatorTasks;   // Number of active virtual thread device tasks
    private int idleCount;
    private int chargingCount;
    private int dischargingCount;
    private int faultCount;
    private Instant timestamp;
}
