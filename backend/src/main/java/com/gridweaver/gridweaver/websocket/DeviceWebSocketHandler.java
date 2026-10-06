package com.gridweaver.gridweaver.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gridweaver.gridweaver.dto.DeviceTelemetryRequest;
import com.gridweaver.gridweaver.service.TelemetryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceWebSocketHandler extends TextWebSocketHandler {

    private final TelemetryService telemetryService;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final AtomicInteger activeDeviceConnections = new AtomicInteger(0);

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        int count = activeDeviceConnections.incrementAndGet();
        if (count % 100 == 0 || count <= 10) {
            log.info("Persistent IoT device WebSocket connected. Active device sockets: {}", count);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        activeDeviceConnections.decrementAndGet();
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            DeviceTelemetryRequest request = objectMapper.readValue(message.getPayload(), DeviceTelemetryRequest.class);
            telemetryService.processTelemetry(request);
        } catch (Exception e) {
            log.error("Failed to parse device telemetry WebSocket message: {}", e.getMessage());
        }
    }

    public int getActiveDeviceConnections() {
        return activeDeviceConnections.get();
    }
}
