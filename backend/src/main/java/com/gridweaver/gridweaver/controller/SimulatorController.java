package com.gridweaver.gridweaver.controller;

import com.gridweaver.gridweaver.simulator.GridSimulatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/simulator")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SimulatorController {

    private final GridSimulatorService simulatorService;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSimulatorStatus() {
        return ResponseEntity.ok(Map.of(
                "activeTasks", simulatorService.getActiveTaskCount(),
                "stormActive", simulatorService.isStormActive(),
                "activeMode", simulatorService.getActiveMode(),
                "currentGridLoad", simulatorService.getCurrentGridLoad()
        ));
    }

    @PostMapping("/mode")
    public ResponseEntity<Map<String, Object>> setMode(@RequestParam com.gridweaver.gridweaver.simulator.SimulationMode mode) {
        simulatorService.setMode(mode);
        return ResponseEntity.ok(Map.of(
                "activeMode", simulatorService.getActiveMode(),
                "stormActive", simulatorService.isStormActive(),
                "currentGridLoad", simulatorService.getCurrentGridLoad()
        ));
    }

    @PostMapping("/scale")
    public ResponseEntity<Map<String, Object>> scaleSimulator(@RequestParam(defaultValue = "10000") int count) {
        int target = Math.max(1, Math.min(100000, count));
        simulatorService.startSimulation(target);
        return ResponseEntity.ok(Map.of(
                "status", "SCALED",
                "deviceCount", target,
                "activeTasks", simulatorService.getActiveTaskCount()
        ));
    }

    @PostMapping("/storm")
    public ResponseEntity<Map<String, Object>> toggleStorm(@RequestParam(required = false) Boolean active) {
        boolean newStatus = (active != null) ? active : !simulatorService.isStormActive();
        simulatorService.setStorm(newStatus);
        return ResponseEntity.ok(Map.of(
                "stormActive", newStatus,
                "currentGridLoad", simulatorService.getCurrentGridLoad()
        ));
    }
}
