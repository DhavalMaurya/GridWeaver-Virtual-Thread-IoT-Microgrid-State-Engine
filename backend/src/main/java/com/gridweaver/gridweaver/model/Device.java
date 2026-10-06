package com.gridweaver.gridweaver.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Device {
    private String id;
    private DeviceType deviceType;
    private double latitude;
    private double longitude;
    private String zoneId;
    private boolean online;
    @lombok.Builder.Default
    private DeviceStatus status = DeviceStatus.REGISTERED;
    @lombok.Builder.Default
    private boolean enabled = true;
    private Instant lastSeen;
    private double power;   // in kW (positive: active flow)
    private double voltage; // in V (e.g., 230V)
    private double current; // in A
}
