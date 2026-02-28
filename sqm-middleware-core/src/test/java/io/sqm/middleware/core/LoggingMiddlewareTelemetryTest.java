package io.sqm.middleware.core;

import io.sqm.middleware.api.DecisionGuidanceDto;
import io.sqm.middleware.api.DecisionKindDto;
import io.sqm.middleware.api.DecisionResultDto;
import io.sqm.middleware.api.ReasonCodeDto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoggingMiddlewareTelemetryTest {

    @Test
    void validates_factory_arguments() {
        var logger = Logger.getLogger("test");
        assertThrows(NullPointerException.class, () -> LoggingMiddlewareTelemetry.of(null));
        assertThrows(NullPointerException.class, () -> LoggingMiddlewareTelemetry.of(logger, null));
    }

    @Test
    void records_decisions_without_errors() {
        var telemetry = LoggingMiddlewareTelemetry.of(Logger.getLogger("test"), Level.FINE);
        var decision = new DecisionResultDto(
            DecisionKindDto.REWRITE,
            ReasonCodeDto.REWRITE_LIMIT,
            "rewritten",
            "select 1 limit 10",
            List.of(),
            "fp",
            new DecisionGuidanceDto(false, null, null, null)
        );
        assertDoesNotThrow(() -> telemetry.record("enforce", decision, 10L));
    }
}
