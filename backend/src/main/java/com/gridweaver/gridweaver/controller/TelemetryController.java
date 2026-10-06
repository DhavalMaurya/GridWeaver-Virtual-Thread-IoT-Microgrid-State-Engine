package com.gridweaver.gridweaver.controller;

import com.gridweaver.gridweaver.dto.DeviceTelemetryRequest;
import com.gridweaver.gridweaver.dto.TelemetryResponse;
import com.gridweaver.gridweaver.service.TelemetryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telemetry")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TelemetryController {

    private final TelemetryService telemetryService;

    @PostMapping
    public ResponseEntity<TelemetryResponse> ingestTelemetry(@Valid @RequestBody DeviceTelemetryRequest request) {
        TelemetryResponse response = telemetryService.processTelemetry(request);
        return ResponseEntity.ok(response);
    }
}
