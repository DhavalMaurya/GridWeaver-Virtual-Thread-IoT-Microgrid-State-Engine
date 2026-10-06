package com.gridweaver.gridweaver.dto;

import com.gridweaver.gridweaver.model.DeviceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceCreateRequest {
    private String id;
    private DeviceType deviceType;
    private Double latitude;
    private Double longitude;
    private String zoneId;
    private Double capacity;
    private Double panelEfficiency;
    private Double maxChargeRate;
    private Double maxDischargeRate;
}
