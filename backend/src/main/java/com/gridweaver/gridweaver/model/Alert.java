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
public class Alert {
    private String id;
    private String deviceId;
    private AlertType type;
    private AlertSeverity severity;
    private String message;
    private Instant timestamp;
    private boolean resolved;
    private Instant resolvedAt;
}
