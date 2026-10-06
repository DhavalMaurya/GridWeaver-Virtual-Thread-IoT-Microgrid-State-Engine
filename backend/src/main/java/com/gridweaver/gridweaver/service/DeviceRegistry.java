package com.gridweaver.gridweaver.service;

import com.gridweaver.gridweaver.model.Battery;
import com.gridweaver.gridweaver.model.Device;
import com.gridweaver.gridweaver.model.DeviceStatus;
import com.gridweaver.gridweaver.model.DeviceType;
import com.gridweaver.gridweaver.model.SolarPanel;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

@Service
public class DeviceRegistry {

    private final ConcurrentHashMap<String, Device> devices = new ConcurrentHashMap<>();
    private final LongAdder telemetryCounter = new LongAdder();
    private final LongAdder stateChangeCounter = new LongAdder();

    private volatile long lastTelemetryRate = 0;
    private volatile long lastStateChangeRate = 0;

    public void save(Device device) {
        if (device != null && device.getId() != null) {
            devices.put(device.getId(), device);
        }
    }

    public Optional<Device> findById(String id) {
        return Optional.ofNullable(devices.get(id));
    }

    public boolean deleteById(String id) {
        return devices.remove(id) != null;
    }

    public List<String> getAvailableZones() {
        return devices.values().stream()
                .map(Device::getZoneId)
                .filter(z -> z != null && !z.isBlank())
                .distinct()
                .sorted()
                .toList();
    }

    public List<Device> checkDeviceHeartbeats(long timeoutSeconds) {
        Instant threshold = Instant.now().minusSeconds(timeoutSeconds);
        List<Device> newlyOffline = new ArrayList<>();
        for (Device device : devices.values()) {
            if (device.isOnline() && device.getLastSeen() != null && device.getLastSeen().isBefore(threshold)) {
                device.setOnline(false);
                device.setStatus(DeviceStatus.OFFLINE);
                newlyOffline.add(device);
            }
        }
        return newlyOffline;
    }

    public List<Device> filterDevices(DeviceType type, String zone, String search, Boolean online) {
        return devices.values().stream()
                .filter(d -> type == null || d.getDeviceType() == type)
                .filter(d -> zone == null || zone.isBlank() || zone.equalsIgnoreCase("ALL") || zone.equalsIgnoreCase(d.getZoneId()))
                .filter(d -> search == null || search.isBlank() || (d.getId() != null && d.getId().toLowerCase().contains(search.toLowerCase().trim())))
                .filter(d -> online == null || d.isOnline() == online)
                .toList();
    }

    public Collection<Device> findAll() {
        return devices.values();
    }

    @SuppressWarnings("unchecked")
    public List<Battery> findAllBatteries() {
        return devices.values().stream()
                .filter(d -> d instanceof Battery)
                .map(d -> (Battery) d)
                .toList();
    }

    @SuppressWarnings("unchecked")
    public List<SolarPanel> findAllSolarPanels() {
        return devices.values().stream()
                .filter(d -> d instanceof SolarPanel)
                .map(d -> (SolarPanel) d)
                .toList();
    }

    public int count() {
        return devices.size();
    }

    public int onlineCount() {
        return (int) devices.values().stream().filter(Device::isOnline).count();
    }

    public void recordTelemetry() {
        telemetryCounter.increment();
    }

    public void recordStateChange() {
        stateChangeCounter.increment();
    }

    public void calculateRates() {
        this.lastTelemetryRate = telemetryCounter.sumThenReset();
        this.lastStateChangeRate = stateChangeCounter.sumThenReset();
    }

    public long getTelemetryRate() {
        return lastTelemetryRate;
    }

    public long getStateChangeRate() {
        return lastStateChangeRate;
    }

    public void clear() {
        devices.clear();
    }
}
