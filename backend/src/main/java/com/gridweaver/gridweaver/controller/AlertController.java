package com.gridweaver.gridweaver.controller;

import com.gridweaver.gridweaver.model.Alert;
import com.gridweaver.gridweaver.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    public ResponseEntity<List<Alert>> getAllAlerts() {
        return ResponseEntity.ok(alertService.getAllAlerts());
    }

    @GetMapping("/active")
    public ResponseEntity<List<Alert>> getActiveAlerts() {
        return ResponseEntity.ok(alertService.getActiveAlerts());
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<Map<String, Object>> resolveAlert(@PathVariable String id) {
        boolean resolved = alertService.resolveAlert(id);
        if (resolved) {
            return ResponseEntity.ok(Map.of("status", "RESOLVED", "alertId", id));
        }
        return ResponseEntity.notFound().build();
    }
}
