package io.sqm.control;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlMiddlewareTest {

    @Test
    void analyze_routes_with_analyze_mode() {
        var modeRef = new AtomicReference<ExecutionMode>();
        var middleware = SqlMiddleware.of((query, context) -> {
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
        var middleware = SqlMiddleware.of((query, context) -> {
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
            (query, context) -> DecisionResult.deny(ReasonCode.DENY_DDL, "ddl blocked"),
            (query, context, decision) -> "reason=%s mode=%s".formatted(decision.reasonCode(), context.mode()));

        var context = ExecutionContext.of("postgresql", ExecutionMode.EXECUTE);
        var explanation = middleware.explainDecision("select 1", context);

        assertEquals(DecisionKind.DENY, explanation.decision().kind());
        assertEquals("reason=DENY_DDL mode=ANALYZE", explanation.explanation());
    }

    @Test
    void pipeline_failure_maps_to_deterministic_deny() {
        var middleware = SqlMiddleware.of((query, context) -> {
            throw new IllegalStateException("parse failed");
        });

        var result = middleware.analyze("select 1", ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));
        assertEquals(DecisionKind.DENY, result.kind());
        assertEquals(ReasonCode.DENY_PIPELINE_ERROR, result.reasonCode());
        assertTrue(result.message().contains("parse failed"));
    }

    @Test
    void emits_audit_event_for_allow_deny_and_rewrite() {
        var events = new ArrayList<AuditEvent>();
        var allowMiddleware = SqlMiddleware.of(
            (query, context) -> DecisionResult.allow("fp-1"),
            events::add
        );
        var rewriteMiddleware = SqlMiddleware.of(
            (query, context) -> DecisionResult.rewrite(ReasonCode.REWRITE_LIMIT, "limit", "select 1 limit 10"),
            events::add
        );
        var denyMiddleware = SqlMiddleware.of(
            (query, context) -> DecisionResult.deny(ReasonCode.DENY_DDL, "blocked"),
            events::add
        );

        var context = ExecutionContext.of("postgresql", "alice", "tenant-a", ExecutionMode.EXECUTE);
        allowMiddleware.analyze("select    1", context);
        rewriteMiddleware.enforce("select 1", context);
        denyMiddleware.analyze("select 1", context);

        assertEquals(3, events.size());

        var allowEvent = events.getFirst();
        assertEquals(DecisionKind.ALLOW, allowEvent.decision().kind());
        assertEquals("select 1", allowEvent.normalizedSql());
        assertEquals(List.of(), allowEvent.appliedRules());
        assertTrue(allowEvent.durationNanos() >= 0);

        var rewriteEvent = events.get(1);
        assertEquals(DecisionKind.REWRITE, rewriteEvent.decision().kind());
        assertEquals("select 1 limit 10", rewriteEvent.rewrittenSql());
        assertEquals("select 1 limit 10", rewriteEvent.normalizedSql());
        assertEquals(List.of(ReasonCode.REWRITE_LIMIT), rewriteEvent.appliedRules());

        var denyEvent = events.get(2);
        assertEquals(DecisionKind.DENY, denyEvent.decision().kind());
        assertEquals(List.of(ReasonCode.DENY_DDL), denyEvent.appliedRules());
    }

    @Test
    void audit_publisher_failure_does_not_change_decision() {
        var middleware = SqlMiddleware.of(
            (query, context) -> DecisionResult.allow(),
            event -> {
                throw new IllegalStateException("sink failed");
            }
        );

        var result = middleware.analyze("select 1", ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));
        assertEquals(DecisionKind.ALLOW, result.kind());
    }

    @Test
    void max_sql_length_guardrail_denies() {
        var middleware = SqlMiddleware.of(
            (query, context) -> DecisionResult.allow(),
            new RuntimeGuardrails(7, null, null, false)
        );

        var result = middleware.analyze("select 1", ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));
        assertEquals(DecisionKind.DENY, result.kind());
        assertEquals(ReasonCode.DENY_MAX_SQL_LENGTH, result.reasonCode());
    }

    @Test
    void timeout_guardrail_denies() {
        var middleware = SqlMiddleware.of(
            (query, context) -> {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                return DecisionResult.allow();
            },
            new RuntimeGuardrails(null, 20L, null, false)
        );

        var result = middleware.enforce("select 1", ExecutionContext.of("postgresql", ExecutionMode.EXECUTE));
        assertEquals(DecisionKind.DENY, result.kind());
        assertEquals(ReasonCode.DENY_TIMEOUT, result.reasonCode());
    }

    @Test
    void max_rows_guardrail_denies_missing_or_excessive_limit() {
        var middleware = SqlMiddleware.of(
            (query, context) -> DecisionResult.allow(),
            new RuntimeGuardrails(null, null, 100, false)
        );
        var context = ExecutionContext.of("postgresql", ExecutionMode.EXECUTE);

        var missingLimit = middleware.enforce("select * from users", context);
        assertEquals(DecisionKind.DENY, missingLimit.kind());
        assertEquals(ReasonCode.DENY_MAX_ROWS, missingLimit.reasonCode());

        var excessiveLimit = middleware.enforce("select * from users limit 500", context);
        assertEquals(DecisionKind.DENY, excessiveLimit.kind());
        assertEquals(ReasonCode.DENY_MAX_ROWS, excessiveLimit.reasonCode());
    }

    @Test
    void explain_dry_run_rewrites_execute_mode() {
        var middleware = SqlMiddleware.of(
            (query, context) -> DecisionResult.allow(),
            new RuntimeGuardrails(null, null, null, true)
        );

        var result = middleware.enforce("select * from users limit 10", ExecutionContext.of("postgresql", ExecutionMode.EXECUTE));
        assertEquals(DecisionKind.REWRITE, result.kind());
        assertEquals(ReasonCode.REWRITE_EXPLAIN_DRY_RUN, result.reasonCode());
        assertEquals("EXPLAIN select * from users limit 10", result.rewrittenSql());
    }

    @Test
    void explain_dry_run_does_not_apply_in_analyze_mode() {
        var middleware = SqlMiddleware.of(
            (query, context) -> DecisionResult.allow("fp-a"),
            new RuntimeGuardrails(null, null, null, true)
        );

        var result = middleware.analyze("select * from users limit 10", ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));
        assertEquals(DecisionKind.ALLOW, result.kind());
        assertEquals(ReasonCode.NONE, result.reasonCode());
        assertEquals("fp-a", result.fingerprint());
    }

    @Test
    void explain_dry_run_uses_existing_rewritten_sql() {
        var middleware = SqlMiddleware.of(
            (query, context) -> DecisionResult.rewrite(ReasonCode.REWRITE_LIMIT, "limit", "select 1 limit 10", "fp-rw"),
            new RuntimeGuardrails(null, null, null, true)
        );

        var result = middleware.enforce("select 1", ExecutionContext.of("postgresql", ExecutionMode.EXECUTE));
        assertEquals(DecisionKind.REWRITE, result.kind());
        assertEquals(ReasonCode.REWRITE_EXPLAIN_DRY_RUN, result.reasonCode());
        assertEquals("EXPLAIN select 1 limit 10", result.rewrittenSql());
        assertEquals("fp-rw", result.fingerprint());
    }

    @Test
    void explanation_falls_back_when_explainer_returns_blank() {
        var middleware = SqlMiddleware.of(
            (query, context) -> DecisionResult.allow(),
            (query, context, decision) -> " "
        );

        var result = middleware.explainDecision("select 1", ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));
        assertEquals("Decision=ALLOW, reason=NONE", result.explanation());
    }

    @Test
    void parse_error_uses_fallback_explanation_and_pipeline_deny() {
        var middleware = SqlMiddleware.of(
            (query, context) -> DecisionResult.allow(),
            (query, context, decision) -> "unused",
            AuditEventPublisher.noop(),
            RuntimeGuardrails.disabled(),
            (sql, context) -> {
                throw new IllegalArgumentException("bad sql");
            }
        );

        var result = middleware.explainDecision("select from", ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));
        assertEquals(DecisionKind.DENY, result.decision().kind());
        assertEquals(ReasonCode.DENY_PIPELINE_ERROR, result.decision().reasonCode());
        assertEquals("Decision=DENY, reason=DENY_PIPELINE_ERROR", result.explanation());
    }

    @Test
    void validate_input_rejects_blank_sql_and_null_context() {
        var middleware = SqlMiddleware.of((query, context) -> DecisionResult.allow());

        assertThrows(IllegalArgumentException.class, () -> middleware.analyze(" ", ExecutionContext.of("postgresql", ExecutionMode.ANALYZE)));
        assertThrows(NullPointerException.class, () -> middleware.analyze("select 1", null));
    }

    @Test
    void null_engine_result_maps_to_pipeline_error() {
        var middleware = SqlMiddleware.of((query, context) -> null);

        var result = middleware.analyze("select 1", ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));
        assertEquals(DecisionKind.DENY, result.kind());
        assertEquals(ReasonCode.DENY_PIPELINE_ERROR, result.reasonCode());
        assertTrue(result.message().contains("engine must return a decision"));
    }

    @Test
    void max_rows_guardrail_ignores_when_engine_already_denied() {
        var middleware = SqlMiddleware.of(
            (query, context) -> DecisionResult.deny(ReasonCode.DENY_DDL, "blocked"),
            new RuntimeGuardrails(null, null, 10, false)
        );

        var result = middleware.enforce("select 1", ExecutionContext.of("postgresql", ExecutionMode.EXECUTE));
        assertEquals(DecisionKind.DENY, result.kind());
        assertEquals(ReasonCode.DENY_DDL, result.reasonCode());
    }

    @Test
    void middleware_factory_overloads_delegate_successfully() {
        var engine = (SqlDecisionEngine) (query, context) -> DecisionResult.allow();
        var context = ExecutionContext.of("postgresql", ExecutionMode.ANALYZE);

        assertEquals(DecisionKind.ALLOW, SqlMiddleware.of(engine).analyze("select 1", context).kind());
        assertEquals(DecisionKind.ALLOW, SqlMiddleware.of(engine, SqlDecisionExplainer.basic()).analyze("select 1", context).kind());
        assertEquals(DecisionKind.ALLOW, SqlMiddleware.of(engine, AuditEventPublisher.noop()).analyze("select 1", context).kind());
        assertEquals(DecisionKind.ALLOW,
            SqlMiddleware.of(engine, SqlDecisionExplainer.basic(), AuditEventPublisher.noop()).analyze("select 1", context).kind());
        assertEquals(DecisionKind.ALLOW,
            SqlMiddleware.of(engine, SqlDecisionExplainer.basic(), AuditEventPublisher.noop(), RuntimeGuardrails.disabled())
                .analyze("select 1", context).kind());
    }
}

