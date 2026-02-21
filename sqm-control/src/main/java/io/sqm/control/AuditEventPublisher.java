package io.sqm.control;

/**
 * Sink contract for middleware audit events.
 */
@FunctionalInterface
public interface AuditEventPublisher {
    /**
     * Publishes one audit event.
     *
     * @param event audit event to publish
     */
    void publish(AuditEvent event);

    /**
     * Returns a no-op publisher.
     *
     * @return no-op audit publisher
     */
    static AuditEventPublisher noop() {
        return event -> {
        };
    }
}

