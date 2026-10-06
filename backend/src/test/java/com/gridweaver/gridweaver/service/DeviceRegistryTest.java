package com.gridweaver.gridweaver.service;

import com.gridweaver.gridweaver.model.Battery;
import com.gridweaver.gridweaver.model.DeviceType;
import com.gridweaver.gridweaver.model.SolarPanel;
import com.gridweaver.gridweaver.state.BatteryState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class DeviceRegistryTest {

    private DeviceRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new DeviceRegistry();
    }

    @Test
    void testSaveAndRetrieveDevices() {
        Battery battery = Battery.builder()
                .id("BATTERY-001")
                .deviceType(DeviceType.BATTERY)
                .latitude(37.7749)
                .longitude(-122.4194)
                .zoneId("ZONE-A")
                .online(true)
                .lastSeen(Instant.now())
                .power(4.5)
                .voltage(230.0)
                .current(19.5)
                .batteryPercentage(85.0)
                .capacity(13.5)
                .temperature(28.5)
                .state(BatteryState.IDLE)
                .build();

        SolarPanel solar = SolarPanel.builder()
                .id("SOLAR-001")
                .deviceType(DeviceType.SOLAR_PANEL)
                .latitude(37.7750)
                .longitude(-122.4195)
                .zoneId("ZONE-A")
                .online(true)
                .lastSeen(Instant.now())
                .power(5.2)
                .voltage(230.0)
                .current(22.6)
                .generationPower(5.2)
                .panelEfficiency(21.0)
                .build();

        registry.save(battery);
        registry.save(solar);

        assertEquals(2, registry.count());
        assertEquals(2, registry.onlineCount());
        assertEquals(1, registry.findAllBatteries().size());
        assertEquals(1, registry.findAllSolarPanels().size());

        assertTrue(registry.findById("BATTERY-001").isPresent());
        Battery retrieved = (Battery) registry.findById("BATTERY-001").get();
        assertEquals(85.0, retrieved.getBatteryPercentage());
        assertEquals(BatteryState.IDLE, retrieved.getState());
    }

    @Test
    void testMetricsCounters() {
        registry.recordTelemetry();
        registry.recordTelemetry();
        registry.recordStateChange();

        registry.calculateRates();

        assertEquals(2, registry.getTelemetryRate());
        assertEquals(1, registry.getStateChangeRate());
    }
}
