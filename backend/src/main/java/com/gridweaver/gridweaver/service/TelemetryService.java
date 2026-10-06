package com.gridweaver.gridweaver.service;

import com.gridweaver.gridweaver.dto.DeviceTelemetryRequest;
import com.gridweaver.gridweaver.dto.TelemetryResponse;
import com.gridweaver.gridweaver.model.AlertSeverity;
import com.gridweaver.gridweaver.model.AlertType;
import com.gridweaver.gridweaver.model.Battery;
import com.gridweaver.gridweaver.model.Device;
import com.gridweaver.gridweaver.model.DeviceStatus;
import com.gridweaver.gridweaver.model.DeviceType;
import com.gridweaver.gridweaver.model.SolarPanel;
import com.gridweaver.gridweaver.state.BatteryState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelemetryService {

    private final DeviceRegistry deviceRegistry;
    private final GridDecisionEngine gridDecisionEngine;
    private final AlertService alertService;

    public TelemetryResponse processTelemetry(DeviceTelemetryRequest request) {
        deviceRegistry.recordTelemetry();

        Instant now = request.getTimestamp() != null ? request.getTimestamp() : Instant.now();
        String deviceId = request.getDeviceId();
        alertService.resolveActiveAlert(deviceId, AlertType.DEVICE_OFFLINE);
        BatteryState currentState = null;

        if (request.getDeviceType() == DeviceType.BATTERY) {
            Battery battery = (Battery) deviceRegistry.findById(deviceId)
                    .filter(d -> d instanceof Battery)
                    .orElseGet(() -> Battery.builder()
                            .id(deviceId)
                            .deviceType(DeviceType.BATTERY)
                            .capacity(13.5)
                            .state(BatteryState.IDLE)
                            .build());

            battery.setLatitude(request.getLatitude());
            battery.setLongitude(request.getLongitude());
            battery.setZoneId(request.getZoneId() != null ? request.getZoneId() : "ZONE-1");
            battery.setOnline(true);
            battery.setStatus(DeviceStatus.ONLINE);
            battery.setLastSeen(now);
            battery.setPower(request.getPower());
            battery.setVoltage(request.getVoltage());
            battery.setCurrent(request.getCurrent());

            if (request.getBatteryPercentage() != null) {
                battery.setBatteryPercentage(request.getBatteryPercentage());
            }
            if (request.getTemperature() != null) {
                battery.setTemperature(request.getTemperature());
            }

            deviceRegistry.save(battery);

            // Evaluate state machine transitions based on gridLoad, temperature, and battery level
            gridDecisionEngine.evaluate(battery, request.getGridLoad());
            currentState = battery.getState();

        } else if (request.getDeviceType() == DeviceType.SOLAR_PANEL) {
            SolarPanel solar = (SolarPanel) deviceRegistry.findById(deviceId)
                    .filter(d -> d instanceof SolarPanel)
                    .orElseGet(() -> SolarPanel.builder()
                            .id(deviceId)
                            .deviceType(DeviceType.SOLAR_PANEL)
                            .panelEfficiency(21.5)
                            .build());

            solar.setLatitude(request.getLatitude());
            solar.setLongitude(request.getLongitude());
            solar.setZoneId(request.getZoneId() != null ? request.getZoneId() : "ZONE-1");
            solar.setOnline(true);
            solar.setStatus(DeviceStatus.ONLINE);
            solar.setLastSeen(now);
            solar.setPower(request.getPower());
            solar.setVoltage(request.getVoltage());
            solar.setCurrent(request.getCurrent());
            solar.setGenerationPower(request.getPower());

            deviceRegistry.save(solar);
        }

        return TelemetryResponse.builder()
                .status("ACCEPTED")
                .deviceId(deviceId)
                .currentState(currentState)
                .processedAt(Instant.now())
                .build();
    }
}
