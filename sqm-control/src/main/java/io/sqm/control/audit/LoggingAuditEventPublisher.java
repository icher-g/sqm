package io.sqm.control.audit;

import io.sqm.control.AuditEvent;
import io.sqm.control.AuditEventPublisher;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Audit event publisher that logs each event via {@link Logger}.
 */
public final class LoggingAuditEventPublisher implements AuditEventPublisher {
    private final Logger logger;
    private final Level level;

    private LoggingAuditEventPublisher(Logger logger, Level level) {
        this.logger = logger;
        this.level = level;
    }

    /**
     * Creates logging publisher with INFO level.
     *
     * @param logger target logger
     * @return publisher instance
     */
    public static LoggingAuditEventPublisher of(Logger logger) {
        return of(logger, Level.INFO);
    }

    /**
     * Creates logging publisher with explicit level.
     *
     * @param logger target logger
     * @param level log level
     * @return publisher instance
     */
    public static LoggingAuditEventPublisher of(Logger logger, Level level) {
        Objects.requireNonNull(logger, "logger must not be null");
        Objects.requireNonNull(level, "level must not be null");
        return new LoggingAuditEventPublisher(logger, level);
    }

    /**
     * Logs one audit event.
     *
     * @param event audit event
     */
    @Override
    public void publish(AuditEvent event) {
        logger.log(level, () -> "sqm-audit decision="
            + event.decision().kind()
            + " reason=" + event.decision().reasonCode()
            + " mode=" + event.context().mode()
            + " sql=" + event.normalizedSql()
            + " durationNanos=" + event.durationNanos());
    }
}

