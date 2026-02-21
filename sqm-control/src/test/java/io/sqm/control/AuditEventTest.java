package io.sqm.control;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuditEventTest {

    @Test
    void constructor_validates_required_fields() {
        assertThrows(IllegalArgumentException.class, () ->
            new AuditEvent(
                " ",
                "select 1",
                List.of(),
                null,
                DecisionResult.allow(),
                ExecutionContext.of("postgresql", ExecutionMode.ANALYZE),
                1L
            ));
        assertThrows(IllegalArgumentException.class, () ->
            new AuditEvent(
                "select 1",
                " ",
                List.of(),
                null,
                DecisionResult.allow(),
                ExecutionContext.of("postgresql", ExecutionMode.ANALYZE),
                1L
            ));
        assertThrows(IllegalArgumentException.class, () ->
            new AuditEvent(
                "select 1",
                "select 1",
                List.of(),
                null,
                DecisionResult.allow(),
                ExecutionContext.of("postgresql", ExecutionMode.ANALYZE),
                -1L
            ));
    }

    @Test
    void applied_rules_is_defensive_copy() {
        var input = new java.util.ArrayList<ReasonCode>();
        input.add(ReasonCode.DENY_DDL);
        var event = new AuditEvent(
            "select 1",
            "select 1",
            input,
            null,
            DecisionResult.allow(),
            ExecutionContext.of("postgresql", ExecutionMode.ANALYZE),
            10L
        );

        input.add(ReasonCode.DENY_DML);
        assertEquals(List.of(ReasonCode.DENY_DDL), event.appliedRules());
    }
}

