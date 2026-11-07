package com.cipher.workspace.Controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@Tag(name = "Health", description = "System health and liveness checks")
@RestController
public class HealthController {

    @Operation(summary = "Liveness and health check endpoint for cloud platforms and orchestrators (e.g. Render, Docker)")
    @GetMapping({"/health", "/api/health"})
    public ResponseEntity<Map<String, Object>> checkHealth() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "timestamp", Instant.now().toString()
        ));
    }
}
