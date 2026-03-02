package io.sqm.control.audit;

/**
 * Sink contract for middleware audit events.
 */
@FunctionalInterface
public interface AuditEventPublisher {
    /**
     * Returns a no-op publisher.
     *
     * @return no-op audit publisher
     */
    static AuditEventPublisher noop() {
        return event -> {
        };
    }

    /**
     * Publishes one audit event.
     *
     * @param event audit event to publish
     */
    void publish(AuditEvent event);
}




