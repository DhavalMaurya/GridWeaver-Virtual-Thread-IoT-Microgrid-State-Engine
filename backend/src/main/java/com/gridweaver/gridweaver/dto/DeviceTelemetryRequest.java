package com.gridweaver.gridweaver.dto;

import com.gridweaver.gridweaver.model.DeviceType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceTelemetryRequest {

    @NotBlank(message = "deviceId cannot be blank")
    private String deviceId;

    @NotNull(message = "deviceType is required")
    private DeviceType deviceType;

    private Instant timestamp;

    @Min(value = -90, message = "latitude must be >= -90")
    @Max(value = 90, message = "latitude must be <= 90")
    private double latitude;

    @Min(value = -180, message = "longitude must be >= -180")
    @Max(value = 180, message = "longitude must be <= 180")
    private double longitude;

    private String zoneId;

    private double power;
    private double voltage;
    private double current;

    @Min(value = 0, message = "batteryPercentage must be >= 0")
    @Max(value = 100, message = "batteryPercentage must be <= 100")
    private Double batteryPercentage;

    private Double temperature;

    @Min(value = 0, message = "gridLoad must be >= 0")
    @Max(value = 100, message = "gridLoad must be <= 100")
    private Double gridLoad; // Simulated control value percentage (0-100)
}
