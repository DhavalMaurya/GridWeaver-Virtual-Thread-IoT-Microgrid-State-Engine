package com.gridweaver.gridweaver.controller;

import com.gridweaver.gridweaver.model.EnergyFlow;
import com.gridweaver.gridweaver.model.GridStatus;
import com.gridweaver.gridweaver.model.SystemMetrics;
import com.gridweaver.gridweaver.service.EnergyFlowService;
import com.gridweaver.gridweaver.service.MetricsService;
import com.gridweaver.gridweaver.simulator.GridSimulatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/grid")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class GridController {

    private final GridSimulatorService simulatorService;
    private final MetricsService metricsService;
    private final EnergyFlowService energyFlowService;

    @GetMapping("/status")
    public ResponseEntity<GridStatus> getGridStatus() {
        return ResponseEntity.ok(simulatorService.getGridStatus());
    }

    @GetMapping("/metrics")
    public ResponseEntity<SystemMetrics> getGridMetrics() {
        return ResponseEntity.ok(metricsService.getSystemMetrics());
    }

    @GetMapping("/energy-flow")
    public ResponseEntity<EnergyFlow> getEnergyFlow() {
        return ResponseEntity.ok(energyFlowService.calculateCurrentFlow());
    }
}
