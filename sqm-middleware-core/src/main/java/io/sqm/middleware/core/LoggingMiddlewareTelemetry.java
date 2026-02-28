package io.sqm.middleware.core;

import io.sqm.middleware.api.DecisionKindDto;
import io.sqm.middleware.api.DecisionResultDto;

import java.util.Objects;
import java.util.concurrent.atomic.LongAdder;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Structured telemetry logger with cumulative counters for throughput and outcome rates.
 */
public final class LoggingMiddlewareTelemetry implements MiddlewareTelemetry {

    private final Logger logger;
    private final Level level;
    private final LongAdder total = new LongAdder();
    private final LongAdder denied = new LongAdder();
    private final LongAdder rewritten = new LongAdder();
    private final LongAdder totalDurationNanos = new LongAdder();

    private LoggingMiddlewareTelemetry(Logger logger, Level level) {
        this.logger = logger;
        this.level = level;
    }

    /**
     * Creates logging telemetry at INFO level.
     *
     * @param logger target logger
     * @return telemetry instance
     */
    public static LoggingMiddlewareTelemetry of(Logger logger) {
        return of(logger, Level.INFO);
    }

    /**
     * Creates logging telemetry with explicit log level.
     *
     * @param logger target logger
     * @param level log level
     * @return telemetry instance
     */
    public static LoggingMiddlewareTelemetry of(Logger logger, Level level) {
        Objects.requireNonNull(logger, "logger must not be null");
        Objects.requireNonNull(level, "level must not be null");
        return new LoggingMiddlewareTelemetry(logger, level);
    }

    /**
     * Records operation latency and decision outcome counters.
     *
     * @param operation operation name
     * @param decision decision payload
     * @param durationNanos operation duration in nanoseconds
     */
    @Override
    public void record(String operation, DecisionResultDto decision, long durationNanos) {
        Objects.requireNonNull(operation, "operation must not be null");
        Objects.requireNonNull(decision, "decision must not be null");
        var safeDurationNanos = Math.max(0L, durationNanos);

        total.increment();
        totalDurationNanos.add(safeDurationNanos);
        if (decision.kind() == DecisionKindDto.DENY) {
            denied.increment();
        } else if (decision.kind() == DecisionKindDto.REWRITE) {
            rewritten.increment();
        }

        var totalValue = total.sum();
        var averageNanos = totalValue == 0 ? 0L : totalDurationNanos.sum() / totalValue;
        logger.log(level, () -> "sqm-metrics operation="
            + operation
            + " kind=" + decision.kind()
            + " reason=" + decision.reasonCode()
            + " durationNanos=" + safeDurationNanos
            + " total=" + totalValue
            + " denied=" + denied.sum()
            + " rewritten=" + rewritten.sum()
            + " avgDurationNanos=" + averageNanos);
    }
}
