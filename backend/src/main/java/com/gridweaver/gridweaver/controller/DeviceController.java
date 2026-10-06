package com.gridweaver.gridweaver.controller;

import com.gridweaver.gridweaver.dto.DeviceCreateRequest;
import com.gridweaver.gridweaver.dto.DeviceResponse;
import com.gridweaver.gridweaver.model.Battery;
import com.gridweaver.gridweaver.model.Device;
import com.gridweaver.gridweaver.model.DeviceStatus;
import com.gridweaver.gridweaver.model.DeviceType;
import com.gridweaver.gridweaver.model.SolarPanel;
import com.gridweaver.gridweaver.state.BatteryState;
import com.gridweaver.gridweaver.service.DeviceRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DeviceController {

    private final DeviceRegistry deviceRegistry;

    @GetMapping
    public ResponseEntity<List<DeviceResponse>> getAllDevices(
            @RequestParam(required = false) DeviceType type,
            @RequestParam(required = false) String zone,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean online
    ) {
        List<DeviceResponse> responses = deviceRegistry.filterDevices(type, zone, search, online).stream()
                .map(DeviceResponse::fromDevice)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{deviceId}")
    public ResponseEntity<DeviceResponse> getDeviceById(@PathVariable String deviceId) {
        return deviceRegistry.findById(deviceId)
                .map(DeviceResponse::fromDevice)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/zones")
    public ResponseEntity<List<String>> getAvailableZones() {
        return ResponseEntity.ok(deviceRegistry.getAvailableZones());
    }

    @PostMapping
    public ResponseEntity<DeviceResponse> registerDevice(@RequestBody DeviceCreateRequest request) {
        String id = request.getId() != null && !request.getId().isBlank()
                ? request.getId()
                : (request.getDeviceType() == DeviceType.BATTERY ? "BAT-" : "SOLAR-") + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        Device device;
        if (request.getDeviceType() == DeviceType.BATTERY) {
            device = Battery.builder()
                    .id(id)
                    .deviceType(DeviceType.BATTERY)
                    .latitude(request.getLatitude() != null ? request.getLatitude() : 37.7749)
                    .longitude(request.getLongitude() != null ? request.getLongitude() : -122.4194)
                    .zoneId(request.getZoneId() != null ? request.getZoneId() : "ZONE-1")
                    .status(DeviceStatus.REGISTERED)
                    .enabled(true)
                    .online(false)
                    .lastSeen(Instant.now())
                    .capacity(request.getCapacity() != null ? request.getCapacity() : 13.5)
                    .batteryPercentage(80.0)
                    .temperature(25.0)
                    .state(BatteryState.IDLE)
                    .maxChargeRate(request.getMaxChargeRate() != null ? request.getMaxChargeRate() : 5.0)
                    .maxDischargeRate(request.getMaxDischargeRate() != null ? request.getMaxDischargeRate() : 5.0)
                    .health("GOOD")
                    .build();
        } else {
            device = SolarPanel.builder()
                    .id(id)
                    .deviceType(DeviceType.SOLAR_PANEL)
                    .latitude(request.getLatitude() != null ? request.getLatitude() : 37.7749)
                    .longitude(request.getLongitude() != null ? request.getLongitude() : -122.4194)
                    .zoneId(request.getZoneId() != null ? request.getZoneId() : "ZONE-1")
                    .status(DeviceStatus.REGISTERED)
                    .enabled(true)
                    .online(false)
                    .lastSeen(Instant.now())
                    .panelEfficiency(request.getPanelEfficiency() != null ? request.getPanelEfficiency() : 21.5)
                    .generationPower(0.0)
                    .build();
        }

        deviceRegistry.save(device);
        return ResponseEntity.status(HttpStatus.CREATED).body(DeviceResponse.fromDevice(device));
    }

    @DeleteMapping("/{deviceId}")
    public ResponseEntity<Void> deleteDevice(@PathVariable String deviceId) {
        boolean removed = deviceRegistry.deleteById(deviceId);
        if (removed) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
