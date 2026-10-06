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
public class TelemetryResponse {
    private String status;
    private String deviceId;
    private BatteryState currentState;
    private Instant processedAt;
}
