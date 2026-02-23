package io.sqm.control.audit;

import io.sqm.control.AuditEvent;
import io.sqm.control.AuditEventPublisher;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe in-memory audit event publisher, useful for tests and local debugging.
 */
public final class InMemoryAuditEventPublisher implements AuditEventPublisher {
    private final CopyOnWriteArrayList<AuditEvent> events = new CopyOnWriteArrayList<>();

    /**
     * Creates a new in-memory publisher.
     */
    public InMemoryAuditEventPublisher() {
    }

    /**
     * Creates a new in-memory publisher.
     *
     * @return publisher instance
     */
    public static InMemoryAuditEventPublisher create() {
        return new InMemoryAuditEventPublisher();
    }

    /**
     * Publishes one audit event by appending it to internal storage.
     *
     * @param event audit event
     */
    @Override
    public void publish(AuditEvent event) {
        events.add(event);
    }

    /**
     * Returns a snapshot of captured events.
     *
     * @return immutable events snapshot
     */
    public List<AuditEvent> events() {
        return List.copyOf(events);
    }

    /**
     * Clears all captured events.
     */
    public void clear() {
        events.clear();
    }
}

