package io.sqm.control;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlMiddlewareTest {

    @Test
    void analyze_routes_with_analyze_mode() {
        var modeRef = new AtomicReference<ExecutionMode>();
        var middleware = SqlMiddleware.of((sql, context) -> {
            modeRef.set(context.mode());
            return DecisionResult.allow();
        });

        var context = ExecutionContext.of("postgresql", "alice", "tenant-a", ExecutionMode.EXECUTE);
        middleware.analyze("select 1", context);

        assertEquals(ExecutionMode.ANALYZE, modeRef.get());
    }

    @Test
    void enforce_routes_with_execute_mode() {
        var modeRef = new AtomicReference<ExecutionMode>();
        var middleware = SqlMiddleware.of((sql, context) -> {
            modeRef.set(context.mode());
            return DecisionResult.allow();
        });

        var context = ExecutionContext.of("postgresql", "alice", "tenant-a", ExecutionMode.ANALYZE);
        middleware.enforce("select 1", context);

        assertEquals(ExecutionMode.EXECUTE, modeRef.get());
    }

    @Test
    void explain_decision_uses_explainer() {
        var middleware = SqlMiddleware.of(
            (sql, context) -> DecisionResult.deny(ReasonCode.DENY_DDL, "ddl blocked"),
            (sql, context, decision) -> "reason=%s mode=%s".formatted(decision.reasonCode(), context.mode()));

        var context = ExecutionContext.of("postgresql", ExecutionMode.EXECUTE);
        var explanation = middleware.explainDecision("drop table users", context);

        assertEquals(DecisionKind.DENY, explanation.decision().kind());
        assertEquals("reason=DENY_DDL mode=ANALYZE", explanation.explanation());
    }

    @Test
    void pipeline_failure_maps_to_deterministic_deny() {
        var middleware = SqlMiddleware.of((sql, context) -> {
            throw new IllegalStateException("parse failed");
        });

        var result = middleware.analyze("select", ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));
        assertEquals(DecisionKind.DENY, result.kind());
        assertEquals(ReasonCode.DENY_PIPELINE_ERROR, result.reasonCode());
        assertTrue(result.message().contains("parse failed"));
    }
}
