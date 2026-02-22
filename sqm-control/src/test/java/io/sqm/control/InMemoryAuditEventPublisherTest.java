package io.sqm.control;

import io.sqm.control.audit.InMemoryAuditEventPublisher;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryAuditEventPublisherTest {

    @Test
    void stores_events_and_returns_snapshot() {
        var publisher = InMemoryAuditEventPublisher.create();
        var event = new AuditEvent(
            "select 1",
            "select 1",
            List.of(),
            null,
            DecisionResult.allow(),
            ExecutionContext.of("postgresql", ExecutionMode.ANALYZE),
            1L
        );

        publisher.publish(event);
        assertEquals(1, publisher.events().size());
        assertEquals(event, publisher.events().getFirst());
    }

    @Test
    void clear_removes_events() {
        var publisher = InMemoryAuditEventPublisher.create();
        publisher.publish(new AuditEvent(
            "select 1",
            "select 1",
            List.of(),
            null,
            DecisionResult.allow(),
            ExecutionContext.of("postgresql", ExecutionMode.ANALYZE),
            1L
        ));

        publisher.clear();
        assertEquals(List.of(), publisher.events());
    }

    @Test
    void returns_immutable_snapshot() {
        var publisher = InMemoryAuditEventPublisher.create();
        publisher.publish(new AuditEvent(
            "select 1",
            "select 1",
            List.of(),
            null,
            DecisionResult.allow(),
            ExecutionContext.of("postgresql", ExecutionMode.ANALYZE),
            1L
        ));

        var snapshot = publisher.events();
        //noinspection DataFlowIssue
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add(snapshot.getFirst()));
    }
}
