package com.gridweaver.gridweaver.websocket;

import com.gridweaver.gridweaver.dto.AlertEvent;
import com.gridweaver.gridweaver.dto.StateChangeEvent;
import com.gridweaver.gridweaver.model.Alert;
import com.gridweaver.gridweaver.model.EnergyFlow;
import com.gridweaver.gridweaver.model.SystemMetrics;
import com.gridweaver.gridweaver.service.DeviceRegistry;
import com.gridweaver.gridweaver.service.EnergyFlowService;
import com.gridweaver.gridweaver.service.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Component
@RequiredArgsConstructor
public class DashboardWebSocketHandler extends TextWebSocketHandler {

    private final MetricsService metricsService;
    private final DeviceRegistry deviceRegistry;
    private final EnergyFlowService energyFlowService;

    private final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("New dashboard WebSocket connected: {} (Total sessions: {})", session.getId(), sessions.size());

        // Immediately send initial state snapshot
        try {
            SystemMetrics metrics = metricsService.getSystemMetrics();
            String initMessage = buildMetricsJson("INITIAL_METRICS", metrics);
            session.sendMessage(new TextMessage(initMessage));
        } catch (Exception e) {
            log.warn("Error sending initial metrics to session {}: {}", session.getId(), e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("Dashboard WebSocket disconnected: {} (Total sessions: {})", session.getId(), sessions.size());
    }

    @EventListener
    public void onStateChange(StateChangeEvent event) {
        if (sessions.isEmpty()) return;

        String json = buildStateChangeJson(event);
        broadcast(json);
    }

    @EventListener
    public void onAlertEvent(AlertEvent event) {
        if (sessions.isEmpty()) return;

        String json = buildAlertJson(event);
        broadcast(json);
    }

    @Scheduled(fixedRate = 1000)
    public void broadcastMetrics() {
        if (sessions.isEmpty()) return;

        SystemMetrics metrics = metricsService.getSystemMetrics();
        String json = buildMetricsJson("GRID_METRICS_UPDATE", metrics);
        broadcast(json);
    }

    private void broadcast(String message) {
        TextMessage textMessage = new TextMessage(message);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    synchronized (session) {
                        session.sendMessage(textMessage);
                    }
                } catch (IOException e) {
                    log.warn("Error sending WebSocket message to session {}: {}", session.getId(), e.getMessage());
                }
            }
        }
    }

    private String buildStateChangeJson(StateChangeEvent event) {
        return "{"
                + "\"eventType\":\"BATTERY_STATE_CHANGED\","
                + "\"deviceId\":\"" + escape(event.getDeviceId()) + "\","
                + "\"previousState\":\"" + event.getPreviousState() + "\","
                + "\"newState\":\"" + event.getNewState() + "\","
                + "\"reason\":\"" + escape(event.getReason()) + "\","
                + "\"gridLoad\":" + (event.getGridLoad() != null ? String.format(Locale.US, "%.2f", event.getGridLoad()) : "0.0") + ","
                + "\"batteryPercentage\":" + (event.getBatteryPercentage() != null ? String.format(Locale.US, "%.2f", event.getBatteryPercentage()) : "0.0") + ","
                + "\"temperature\":" + (event.getTemperature() != null ? String.format(Locale.US, "%.2f", event.getTemperature()) : "0.0") + ","
                + "\"power\":" + (event.getPower() != null ? String.format(Locale.US, "%.2f", event.getPower()) : "0.0") + ","
                + "\"timestamp\":\"" + (event.getTimestamp() != null ? event.getTimestamp().toString() : "") + "\""
                + "}";
    }

    private String buildAlertJson(AlertEvent event) {
        Alert a = event.getAlert();
        return "{"
                + "\"eventType\":\"" + event.getEventType() + "\","
                + "\"alert\":{"
                + "\"id\":\"" + escape(a.getId()) + "\","
                + "\"deviceId\":\"" + escape(a.getDeviceId()) + "\","
                + "\"type\":\"" + a.getType().name() + "\","
                + "\"severity\":\"" + a.getSeverity().name() + "\","
                + "\"message\":\"" + escape(a.getMessage()) + "\","
                + "\"resolved\":" + a.isResolved() + ","
                + "\"timestamp\":\"" + (a.getTimestamp() != null ? a.getTimestamp().toString() : "") + "\","
                + "\"resolvedAt\":\"" + (a.getResolvedAt() != null ? a.getResolvedAt().toString() : "") + "\""
                + "}"
                + "}";
    }

    private String buildMetricsJson(String eventType, SystemMetrics m) {
        EnergyFlow ef = energyFlowService.calculateCurrentFlow();
        return "{"
                + "\"eventType\":\"" + eventType + "\","
                + "\"totalDevices\":" + m.getTotalDevices() + ","
                + "\"onlineDevices\":" + m.getOnlineDevices() + ","
                + "\"telemetryPerSec\":" + m.getTelemetryPerSec() + ","
                + "\"stateChangesPerSec\":" + m.getStateChangesPerSec() + ","
                + "\"gridLoad\":" + String.format(Locale.US, "%.2f", m.getGridLoad()) + ","
                + "\"activeSimulatorTasks\":" + m.getActiveSimulatorTasks() + ","
                + "\"idleCount\":" + m.getIdleCount() + ","
                + "\"chargingCount\":" + m.getChargingCount() + ","
                + "\"dischargingCount\":" + m.getDischargingCount() + ","
                + "\"faultCount\":" + m.getFaultCount() + ","
                + "\"timestamp\":\"" + (m.getTimestamp() != null ? m.getTimestamp().toString() : "") + "\","
                + "\"energyFlow\":{"
                + "\"solarGeneration\":" + String.format(Locale.US, "%.2f", ef.getSolarGeneration()) + ","
                + "\"batteryCharging\":" + String.format(Locale.US, "%.2f", ef.getBatteryCharging()) + ","
                + "\"batteryDischarging\":" + String.format(Locale.US, "%.2f", ef.getBatteryDischarging()) + ","
                + "\"gridConsumption\":" + String.format(Locale.US, "%.2f", ef.getGridConsumption()) + ","
                + "\"netPowerFlow\":" + String.format(Locale.US, "%.2f", ef.getNetPowerFlow()) + ","
                + "\"timestamp\":\"" + (ef.getTimestamp() != null ? ef.getTimestamp().toString() : "") + "\""
                + "}"
                + "}";
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
