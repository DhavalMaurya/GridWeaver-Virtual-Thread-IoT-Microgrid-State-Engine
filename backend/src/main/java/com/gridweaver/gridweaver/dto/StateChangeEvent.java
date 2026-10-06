package com.gridweaver.gridweaver.dto;

import com.gridweaver.gridweaver.state.BatteryState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StateChangeEvent {
    @Builder.Default
    private String eventType = "BATTERY_STATE_CHANGED";
    private String deviceId;
    private BatteryState previousState;
    private BatteryState newState;
    private String reason;
    private Double gridLoad;
    private Instant timestamp;
    private Double batteryPercentage;
    private Double temperature;
    private Double power;
}
