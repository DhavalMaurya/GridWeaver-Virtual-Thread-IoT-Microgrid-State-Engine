package com.gridweaver.gridweaver.service;

import com.gridweaver.gridweaver.dto.AlertEvent;
import com.gridweaver.gridweaver.model.Alert;
import com.gridweaver.gridweaver.model.AlertSeverity;
import com.gridweaver.gridweaver.model.AlertType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final ApplicationEventPublisher eventPublisher;

    private final ConcurrentHashMap<String, Alert> allAlerts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> activeIncidentKeys = new ConcurrentHashMap<>();

    public Alert createAlert(String deviceId, AlertType type, AlertSeverity severity, String message) {
        String key = (deviceId != null ? deviceId : "GLOBAL") + "_" + type.name();
        String existingId = activeIncidentKeys.get(key);

        if (existingId != null) {
            Alert existing = allAlerts.get(existingId);
            if (existing != null && !existing.isResolved()) {
                existing.setMessage(message);
                if (severity == AlertSeverity.CRITICAL) {
                    existing.setSeverity(severity);
                }
                return existing;
            }
        }

        String alertId = UUID.randomUUID().toString();
        Alert alert = Alert.builder()
                .id(alertId)
                .deviceId(deviceId)
                .type(type)
                .severity(severity)
                .message(message)
                .timestamp(Instant.now())
                .resolved(false)
                .build();

        allAlerts.put(alertId, alert);
        activeIncidentKeys.put(key, alertId);

        log.warn("🚨 [ALERT_CREATED] [{}] Device: {} - {}", severity, deviceId, message);
        eventPublisher.publishEvent(new AlertEvent("ALERT_CREATED", alert));
        return alert;
    }

    public boolean resolveAlert(String alertId) {
        Alert alert = allAlerts.get(alertId);
        if (alert != null && !alert.isResolved()) {
            alert.setResolved(true);
            alert.setResolvedAt(Instant.now());

            String key = (alert.getDeviceId() != null ? alert.getDeviceId() : "GLOBAL") + "_" + alert.getType().name();
            activeIncidentKeys.remove(key, alertId);

            log.info("✅ [ALERT_RESOLVED] Alert {} resolved: {}", alertId, alert.getMessage());
            eventPublisher.publishEvent(new AlertEvent("ALERT_RESOLVED", alert));
            return true;
        }
        return false;
    }

    public void resolveActiveAlert(String deviceId, AlertType type) {
        String key = (deviceId != null ? deviceId : "GLOBAL") + "_" + type.name();
        String alertId = activeIncidentKeys.remove(key);
        if (alertId != null) {
            Alert alert = allAlerts.get(alertId);
            if (alert != null && !alert.isResolved()) {
                alert.setResolved(true);
                alert.setResolvedAt(Instant.now());
                log.info("✅ [ALERT_RESOLVED] Alert {} auto-cleared for {}: {}", alertId, deviceId, type);
                eventPublisher.publishEvent(new AlertEvent("ALERT_RESOLVED", alert));
            }
        }
    }

    public List<Alert> getActiveAlerts() {
        return allAlerts.values().stream()
                .filter(a -> !a.isResolved())
                .sorted(Comparator.comparing(Alert::getTimestamp).reversed())
                .toList();
    }

    public List<Alert> getAllAlerts() {
        return allAlerts.values().stream()
                .sorted(Comparator.comparing(Alert::getTimestamp).reversed())
                .toList();
    }
}
