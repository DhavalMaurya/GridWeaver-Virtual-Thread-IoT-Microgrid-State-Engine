package com.gridweaver.gridweaver.dto;

import com.gridweaver.gridweaver.model.Alert;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertEvent {
    private String eventType; // "ALERT_CREATED" or "ALERT_RESOLVED"
    private Alert alert;
}
