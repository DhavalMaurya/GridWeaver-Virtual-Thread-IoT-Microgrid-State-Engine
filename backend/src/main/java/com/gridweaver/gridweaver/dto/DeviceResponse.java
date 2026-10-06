package com.gridweaver.gridweaver.dto;

import com.gridweaver.gridweaver.model.Battery;
import com.gridweaver.gridweaver.model.Device;
import com.gridweaver.gridweaver.model.DeviceType;
import com.gridweaver.gridweaver.model.SolarPanel;
import com.gridweaver.gridweaver.state.BatteryState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceResponse {
    private String id;
    private DeviceType deviceType;
    private double latitude;
    private double longitude;
    private String zoneId;
    private boolean online;
    private com.gridweaver.gridweaver.model.DeviceStatus status;
    private Boolean enabled;
    private Instant lastSeen;
    private double power;
    private double voltage;
    private double current;

    // Battery specific
    private Double batteryPercentage;
    private Double capacity;
    private Double temperature;
    private BatteryState state;
    private Double maxChargeRate;
    private Double maxDischargeRate;
    private String health;

    // Solar specific
    private Double generationPower;
    private Double panelEfficiency;

    public static DeviceResponse fromDevice(Device device) {
        if (device == null) return null;

        DeviceResponseBuilder builder = DeviceResponse.builder()
                .id(device.getId())
                .deviceType(device.getDeviceType())
                .latitude(device.getLatitude())
                .longitude(device.getLongitude())
                .zoneId(device.getZoneId())
                .online(device.isOnline())
                .status(device.getStatus())
                .enabled(device.isEnabled())
                .lastSeen(device.getLastSeen())
                .power(device.getPower())
                .voltage(device.getVoltage())
                .current(device.getCurrent());

        if (device instanceof Battery battery) {
            builder.batteryPercentage(battery.getBatteryPercentage())
                    .capacity(battery.getCapacity())
                    .temperature(battery.getTemperature())
                    .state(battery.getState())
                    .maxChargeRate(battery.getMaxChargeRate())
                    .maxDischargeRate(battery.getMaxDischargeRate())
                    .health(battery.getHealth());
        } else if (device instanceof SolarPanel solar) {
            builder.generationPower(solar.getGenerationPower())
                    .panelEfficiency(solar.getPanelEfficiency());
        }

        return builder.build();
    }
}
