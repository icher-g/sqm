package io.sqm.middleware.core;

import io.sqm.middleware.api.DecisionGuidanceDto;
import io.sqm.middleware.api.DecisionKindDto;
import io.sqm.middleware.api.DecisionResultDto;
import io.sqm.middleware.api.ReasonCodeDto;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @Test
    void record_rejects_null_arguments() {
        var telemetry = LoggingMiddlewareTelemetry.of(Logger.getLogger("test"), Level.INFO);
        var decision = new DecisionResultDto(DecisionKindDto.ALLOW, ReasonCodeDto.NONE, null, null, List.of(), null, null);
        assertThrows(NullPointerException.class, () -> telemetry.record(null, decision, 1L));
        assertThrows(NullPointerException.class, () -> telemetry.record("analyze", null, 1L));
    }

    @Test
    void negative_duration_is_clamped_and_counters_are_logged() {
        var logger = Logger.getLogger("io.sqm.middleware.core.telemetry.test");
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);
        var records = new ArrayList<LogRecord>();
        var handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                records.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        logger.addHandler(handler);
        try {
            var telemetry = LoggingMiddlewareTelemetry.of(logger, Level.INFO);
            telemetry.record("analyze", new DecisionResultDto(DecisionKindDto.DENY, ReasonCodeDto.DENY_DDL, null, null, List.of(), null, null), -10L);
            telemetry.record("analyze", new DecisionResultDto(DecisionKindDto.ALLOW, ReasonCodeDto.NONE, null, null, List.of(), null, null), 5L);

            assertTrue(records.size() >= 2);
            var firstMessage = records.getFirst().getMessage();
            assertTrue(firstMessage.contains("durationNanos=0"));
            assertTrue(firstMessage.contains("denied=1"));
        } finally {
            logger.removeHandler(handler);
        }
    }
}
