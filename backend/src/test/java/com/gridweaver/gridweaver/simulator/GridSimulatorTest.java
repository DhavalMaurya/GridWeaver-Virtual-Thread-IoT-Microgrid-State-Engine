package com.gridweaver.gridweaver.simulator;

import com.gridweaver.gridweaver.model.Battery;
import com.gridweaver.gridweaver.model.SolarPanel;
import com.gridweaver.gridweaver.service.DeviceRegistry;
import com.gridweaver.gridweaver.state.BatteryState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class GridSimulatorTest {

    @Autowired
    private GridSimulatorService simulatorService;

    @Autowired
    private DeviceRegistry deviceRegistry;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        simulatorService.stopSimulation();
        deviceRegistry.clear();
    }

    @AfterEach
    void tearDown() {
        simulatorService.stopSimulation();
        deviceRegistry.clear();
    }

    @Test
    void testVirtualThreadSimulationWith10Devices() throws InterruptedException {
        // Start 10 devices using Java 21 Virtual Threads
        simulatorService.startSimulation(10);
        assertEquals(10, simulatorService.getActiveTaskCount());

        // Wait for virtual threads to generate at least 1-2 telemetry iterations
        Thread.sleep(2200);

        assertEquals(10, deviceRegistry.count(), "10 simulated devices should be registered in DeviceRegistry");
        List<Battery> batteries = deviceRegistry.findAllBatteries();
        List<SolarPanel> solarPanels = deviceRegistry.findAllSolarPanels();

        assertEquals(5, batteries.size(), "Should have 5 batteries");
        assertEquals(5, solarPanels.size(), "Should have 5 solar panels");

        assertTrue(deviceRegistry.onlineCount() >= 10, "All simulated devices should be online");

        // Test Storm Trigger
        simulatorService.setStorm(true);
        assertTrue(simulatorService.isStormActive());
        assertTrue(simulatorService.getCurrentGridLoad() > 80.0, "Storm mode should surge simulated gridLoad > 80%");

        // Wait for next telemetry cycle so batteries react to high load
        Thread.sleep(2200);

        boolean anyDischarging = batteries.stream()
                .anyMatch(b -> b.getState() == BatteryState.DISCHARGING);
        assertTrue(anyDischarging, "Under storm conditions (>80% gridLoad), batteries should transition to DISCHARGING");

        // Reset storm
        simulatorService.setStorm(false);
        assertFalse(simulatorService.isStormActive());
    }
}
