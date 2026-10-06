package com.gridweaver.gridweaver.service;

import com.gridweaver.gridweaver.model.AlertSeverity;
import com.gridweaver.gridweaver.model.AlertType;
import com.gridweaver.gridweaver.model.Device;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceStatusService {

    private final DeviceRegistry deviceRegistry;
    private final AlertService alertService;

    @Value("${gridweaver.devices.offline-timeout-seconds:5}")
    private long offlineTimeoutSeconds;

    @Scheduled(fixedRate = 2000)
    public void monitorDeviceHeartbeats() {
        List<Device> newlyOffline = deviceRegistry.checkDeviceHeartbeats(offlineTimeoutSeconds);
        for (Device device : newlyOffline) {
            log.warn("Device {} marked OFFLINE (timeout: {}s)", device.getId(), offlineTimeoutSeconds);
            alertService.createAlert(
                    device.getId(),
                    AlertType.DEVICE_OFFLINE,
                    AlertSeverity.WARNING,
                    "Device " + device.getId() + " offline: no telemetry received in >" + offlineTimeoutSeconds + "s"
            );
        }
    }
}
