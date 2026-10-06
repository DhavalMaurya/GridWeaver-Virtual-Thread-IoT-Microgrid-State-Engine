package com.gridweaver.gridweaver.state;

import com.gridweaver.gridweaver.dto.DeviceTelemetryRequest;
import com.gridweaver.gridweaver.model.Battery;
import com.gridweaver.gridweaver.model.DeviceType;
import com.gridweaver.gridweaver.service.DeviceRegistry;
import com.gridweaver.gridweaver.service.TelemetryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BatteryStateMachineTest {

    @Autowired
    private TelemetryService telemetryService;

    @Autowired
    private BatteryStateService batteryStateService;

    @Autowired
    private DeviceRegistry deviceRegistry;

    @BeforeEach
    void setUp() {
        deviceRegistry.clear();
    }

    @Test
    void testSpringStateMachineTransitionsViaTelemetry() {
        String deviceId = "BATTERY-TEST-01";

        // Step 1: Initial telemetry under normal load (60%) -> battery stays IDLE
        telemetryService.processTelemetry(DeviceTelemetryRequest.builder()
                .deviceId(deviceId)
                .deviceType(DeviceType.BATTERY)
                .timestamp(Instant.now())
                .batteryPercentage(80.0)
                .temperature(25.0)
                .gridLoad(60.0)
                .build());

        Battery battery = (Battery) deviceRegistry.findById(deviceId).orElseThrow();
        assertEquals(BatteryState.IDLE, battery.getState());

        // Step 2: High Grid Load (85% > 80%) -> Transitions IDLE -> DISCHARGING
        telemetryService.processTelemetry(DeviceTelemetryRequest.builder()
                .deviceId(deviceId)
                .deviceType(DeviceType.BATTERY)
                .timestamp(Instant.now())
                .batteryPercentage(80.0)
                .temperature(26.0)
                .gridLoad(85.0)
                .build());

        battery = (Battery) deviceRegistry.findById(deviceId).orElseThrow();
        assertEquals(BatteryState.DISCHARGING, battery.getState(), "Battery should transition to DISCHARGING when grid load > 80%");

        // Step 3: High Temperature (55°C > 50°C) -> Transitions DISCHARGING -> FAULT
        telemetryService.processTelemetry(DeviceTelemetryRequest.builder()
                .deviceId(deviceId)
                .deviceType(DeviceType.BATTERY)
                .timestamp(Instant.now())
                .batteryPercentage(78.0)
                .temperature(55.0)
                .gridLoad(85.0)
                .build());

        battery = (Battery) deviceRegistry.findById(deviceId).orElseThrow();
        assertEquals(BatteryState.FAULT, battery.getState(), "Battery should transition to FAULT when temperature > 50°C");

        // Step 4: Invalid Transition Attempt in FAULT state -> Spring State Machine must reject START_CHARGING
        boolean eventResult = batteryStateService.fireEvent(deviceId, BatteryEvent.START_CHARGING, "Invalid test trigger", 30.0);
        assertFalse(eventResult, "Spring State Machine must reject invalid transition from FAULT to CHARGING");
        battery = (Battery) deviceRegistry.findById(deviceId).orElseThrow();
        assertEquals(BatteryState.FAULT, battery.getState(), "State must remain FAULT after invalid event");

        // Step 5: Temperature Cools Down (45°C <= 50°C) -> FAULT_CLEARED returns battery to IDLE
        telemetryService.processTelemetry(DeviceTelemetryRequest.builder()
                .deviceId(deviceId)
                .deviceType(DeviceType.BATTERY)
                .timestamp(Instant.now())
                .batteryPercentage(78.0)
                .temperature(45.0)
                .gridLoad(60.0)
                .build());

        battery = (Battery) deviceRegistry.findById(deviceId).orElseThrow();
        assertEquals(BatteryState.IDLE, battery.getState(), "Battery should return to IDLE on FAULT_CLEARED when temperature <= 50°C");

        // Step 6: Low Grid Load (35% < 40%) -> Transitions IDLE -> CHARGING
        telemetryService.processTelemetry(DeviceTelemetryRequest.builder()
                .deviceId(deviceId)
                .deviceType(DeviceType.BATTERY)
                .timestamp(Instant.now())
                .batteryPercentage(75.0)
                .temperature(28.0)
                .gridLoad(35.0)
                .build());

        battery = (Battery) deviceRegistry.findById(deviceId).orElseThrow();
        assertEquals(BatteryState.CHARGING, battery.getState(), "Battery should transition to CHARGING when grid load < 40%");
    }
}
