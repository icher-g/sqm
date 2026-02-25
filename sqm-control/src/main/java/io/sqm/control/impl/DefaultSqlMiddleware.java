package io.sqm.control.impl;

import io.sqm.control.*;
import io.sqm.core.*;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Default {@link SqlMiddleware} implementation that orchestrates parse, decision evaluation,
 * runtime guardrails, explanation, and audit publishing.
 */
public final class DefaultSqlMiddleware implements SqlMiddleware {

    private final SqlDecisionEngine engine;
    private final SqlDecisionExplainer explainer;
    private final AuditEventPublisher auditPublisher;
    private final RuntimeGuardrails guardrails;
    private final SqlQueryParser queryParser;

    /**
     * Creates a middleware instance with explicit component wiring.
     *
     * @param engine         decision engine used for validate/rewrite/render decisions
     * @param explainer      explainer used by {@link #explainDecision(String, ExecutionContext)}
     * @param auditPublisher audit sink for emitted audit events
     * @param guardrails     runtime guardrail settings
     * @param queryParser    parser used for ingress SQL parsing
     */
    public DefaultSqlMiddleware(
        SqlDecisionEngine engine,
        SqlDecisionExplainer explainer,
        AuditEventPublisher auditPublisher,
        RuntimeGuardrails guardrails,
        SqlQueryParser queryParser
    ) {
        this.engine = engine;
        this.explainer = explainer;
        this.auditPublisher = auditPublisher;
        this.guardrails = guardrails;
        this.queryParser = queryParser;
    }

    private static Long extractLimit(Query query) {
        return switch (query) {
            case SelectQuery select -> extractLimit(select.limitOffset());
            case CompositeQuery composite -> extractLimit(composite.limitOffset());
            case WithQuery with -> with.body() == null ? null : extractLimit(with.body());
            default -> null;
        };
    }

    private static Long extractLimit(LimitOffset limitOffset) {
        if (limitOffset == null || limitOffset.limitAll() || limitOffset.limit() == null) {
            return null;
        }
        return switch (limitOffset.limit()) {
            case LiteralExpr literalExpr when literalExpr.value() instanceof Number n -> n.longValue();
            default -> null;
        };
    }

    private static void validateInput(String sql, ExecutionContext context) {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("sql must not be blank");
        }
        Objects.requireNonNull(context, "context must not be null");
    }

    private static String normalizeForAudit(String sql) {
        return sql.trim().replaceAll("\\s+", " ");
    }

    @Override
    public DecisionResult analyze(String sql, ExecutionContext context) {
        return evaluate(sql, context, ExecutionMode.ANALYZE).decision();
    }

    @Override
    public DecisionResult enforce(String sql, ExecutionContext context) {
        return evaluate(sql, context, ExecutionMode.EXECUTE).decision();
    }

    @Override
    public DecisionExplanation explainDecision(String sql, ExecutionContext context) {
        var evaluated = evaluate(sql, context, ExecutionMode.ANALYZE);
        var decision = evaluated.decision();
        var explanation = evaluated.query() == null
            ? null
            : explainer.explain(evaluated.query(), context.withExecutionMode(ExecutionMode.ANALYZE), decision);
        if (explanation == null || explanation.isBlank()) {
            explanation = "Decision=%s, reason=%s".formatted(decision.kind(), decision.reasonCode());
        }
        return new DecisionExplanation(decision, explanation);
    }

    private EvaluatedDecision evaluate(String sql, ExecutionContext context, ExecutionMode mode) {
        validateInput(sql, context);
        var contextWithMode = context.withExecutionMode(mode);
        var startedNanos = System.nanoTime();
        Query query = null;

        var decision = precheckSqlLength(sql);
        if (decision == null) {
            try {
                query = queryParser.parse(sql, contextWithMode);
            } catch (RuntimeException ex) {
                decision = DecisionResult.deny(ReasonCode.DENY_PIPELINE_ERROR, ex.getMessage());
            }
        }
        if (decision == null) {
            decision = evaluateWithTimeout(query, contextWithMode);
            decision = applyMaxRowsGuardrail(query, decision);
            decision = applyExplainDryRun(sql, mode, decision);
        }

        emitAuditEvent(sql, contextWithMode, decision, System.nanoTime() - startedNanos);
        return new EvaluatedDecision(decision, query);
    }

    private DecisionResult precheckSqlLength(String sql) {
        if (guardrails.maxSqlLength() == null) {
            return null;
        }
        if (sql.length() <= guardrails.maxSqlLength()) {
            return null;
        }
        return DecisionResult.deny(
            ReasonCode.DENY_MAX_SQL_LENGTH,
            "SQL length %d exceeds configured max %d".formatted(sql.length(), guardrails.maxSqlLength())
        );
    }

    private DecisionResult evaluateWithTimeout(Query query, ExecutionContext contextWithMode) {
        if (guardrails.timeoutMillis() == null) {
            try {
                return Objects.requireNonNull(engine.evaluate(query, contextWithMode), "engine must return a decision");
            } catch (RuntimeException ex) {
                return DecisionResult.deny(ReasonCode.DENY_PIPELINE_ERROR, ex.getMessage());
            }
        }

        try (var executor = Executors.newSingleThreadExecutor()) {
            var future = executor.submit(() -> Objects.requireNonNull(engine.evaluate(query, contextWithMode), "engine must return a decision"));
            try {
                return future.get(guardrails.timeoutMillis(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException ex) {
                future.cancel(true);
                return DecisionResult.deny(
                    ReasonCode.DENY_TIMEOUT,
                    "Evaluation exceeded timeout of %d ms".formatted(guardrails.timeoutMillis())
                );
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return DecisionResult.deny(
                    ReasonCode.DENY_TIMEOUT,
                    "Evaluation interrupted while waiting for decision"
                );
            } catch (ExecutionException ex) {
                var cause = ex.getCause();
                var message = cause == null ? ex.getMessage() : cause.getMessage();
                return DecisionResult.deny(ReasonCode.DENY_PIPELINE_ERROR, message);
            }
        }
    }

    private DecisionResult applyMaxRowsGuardrail(Query query, DecisionResult decision) {
        if (guardrails.maxRows() == null || decision.kind() == DecisionKind.DENY) {
            return decision;
        }

        var detectedLimit = extractLimit(query);
        if (detectedLimit == null) {
            return DecisionResult.deny(
                ReasonCode.DENY_MAX_ROWS,
                "Query must include LIMIT <= %d".formatted(guardrails.maxRows())
            );
        }
        if (detectedLimit > guardrails.maxRows()) {
            return DecisionResult.deny(
                ReasonCode.DENY_MAX_ROWS,
                "Query LIMIT %d exceeds configured maxRows %d".formatted(detectedLimit, guardrails.maxRows())
            );
        }
        return decision;
    }

    private DecisionResult applyExplainDryRun(String originalSql, ExecutionMode mode, DecisionResult decision) {
        if (!guardrails.explainDryRun() || mode != ExecutionMode.EXECUTE || decision.kind() == DecisionKind.DENY) {
            return decision;
        }
        var effectiveSql = decision.rewrittenSql() == null ? originalSql.trim() : decision.rewrittenSql().trim();
        return DecisionResult.rewrite(
            ReasonCode.REWRITE_EXPLAIN_DRY_RUN,
            "Execution switched to EXPLAIN dry-run",
            "EXPLAIN " + effectiveSql,
            decision.sqlParams(),
            decision.fingerprint()
        );
    }

    private void emitAuditEvent(String rawSql, ExecutionContext context, DecisionResult decision, long durationNanos) {
        var normalizedSql = normalizeForAudit(decision.rewrittenSql() == null ? rawSql : decision.rewrittenSql());
        var appliedRules = decision.reasonCode() == ReasonCode.NONE
            ? List.<ReasonCode>of()
            : List.of(decision.reasonCode());
        var event = new AuditEvent(
            rawSql,
            normalizedSql,
            appliedRules,
            decision.rewrittenSql(),
            decision,
            context,
            durationNanos
        );
        try {
            auditPublisher.publish(event);
        } catch (RuntimeException ignored) {
            // Observability must not alter middleware decision behavior.
        }
    }

    private record EvaluatedDecision(DecisionResult decision, Query query) {
    }
}
