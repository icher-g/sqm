package io.sqm.playground.rest.controller;

import io.sqm.playground.api.HealthResponseDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * HTTP controller exposing playground host health.
 */
@RestController
@RequestMapping("/api/v1")
public final class HealthController {

    /**
     * Creates the health controller.
     */
    public HealthController() {
    }

    /**
     * Returns liveness-style host health.
     *
     * @return playground health response
     */
    @GetMapping("/health")
    public HealthResponseDto health() {
        return new HealthResponseDto(
            UUID.randomUUID().toString(),
            true,
            0L,
            "UP",
            List.of()
        );
    }
}
