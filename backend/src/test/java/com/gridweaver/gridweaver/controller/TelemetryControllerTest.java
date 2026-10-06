package com.gridweaver.gridweaver.controller;

import com.gridweaver.gridweaver.dto.DeviceResponse;
import com.gridweaver.gridweaver.dto.DeviceTelemetryRequest;
import com.gridweaver.gridweaver.dto.TelemetryResponse;
import com.gridweaver.gridweaver.model.Battery;
import com.gridweaver.gridweaver.model.DeviceType;
import com.gridweaver.gridweaver.service.DeviceRegistry;
import com.gridweaver.gridweaver.state.BatteryState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TelemetryControllerTest {

    @Autowired
    private TelemetryController telemetryController;

    @Autowired
    private DeviceController deviceController;

    @Autowired
    private DeviceRegistry deviceRegistry;

    @BeforeEach
    void setUp() {
        deviceRegistry.clear();
    }

    @Test
    void testSingleBatteryTelemetryIngestion() {
        DeviceTelemetryRequest request = DeviceTelemetryRequest.builder()
                .deviceId("BATTERY-00042")
                .deviceType(DeviceType.BATTERY)
                .timestamp(Instant.now())
                .latitude(37.7749)
                .longitude(-122.4194)
                .zoneId("ZONE-1")
                .power(3.8)
                .voltage(230.0)
                .current(16.5)
                .batteryPercentage(73.0)
                .temperature(31.0)
                .gridLoad(67.0)
                .build();

        ResponseEntity<TelemetryResponse> response = telemetryController.ingestTelemetry(request);
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("ACCEPTED", response.getBody().getStatus());
        assertEquals("BATTERY-00042", response.getBody().getDeviceId());
        assertEquals(BatteryState.IDLE, response.getBody().getCurrentState());

        // Verify device registry
        assertTrue(deviceRegistry.findById("BATTERY-00042").isPresent());
        Battery battery = (Battery) deviceRegistry.findById("BATTERY-00042").get();
        assertEquals(73.0, battery.getBatteryPercentage());
        assertEquals(31.0, battery.getTemperature());
        assertEquals(3.8, battery.getPower());
        assertEquals(BatteryState.IDLE, battery.getState());
        assertTrue(battery.isOnline());

        // Test GET /api/devices/{deviceId}
        ResponseEntity<DeviceResponse> getResponse = deviceController.getDeviceById("BATTERY-00042");
        assertNotNull(getResponse);
        assertEquals(200, getResponse.getStatusCode().value());
        assertNotNull(getResponse.getBody());
        assertEquals("BATTERY-00042", getResponse.getBody().getId());
        assertEquals(DeviceType.BATTERY, getResponse.getBody().getDeviceType());
        assertEquals(73.0, getResponse.getBody().getBatteryPercentage());
        assertEquals(BatteryState.IDLE, getResponse.getBody().getState());
    }
}
