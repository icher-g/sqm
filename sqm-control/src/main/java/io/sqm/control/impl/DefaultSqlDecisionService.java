package io.sqm.control.impl;

import io.sqm.control.audit.*;
import io.sqm.control.config.*;
import io.sqm.control.decision.*;
import io.sqm.control.execution.*;
import io.sqm.control.pipeline.*;
import io.sqm.control.service.*;

import io.sqm.core.*;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Default {@link SqlDecisionService} implementation that orchestrates parse, decision evaluation,
 * runtime guardrails, explanation, and audit publishing.
 */
public final class DefaultSqlDecisionService implements SqlDecisionService {

    private final SqlDecisionEngine engine;
    private final SqlDecisionExplainer explainer;
    private final AuditEventPublisher auditPublisher;
    private final RuntimeGuardrails guardrails;
    private final SqlStatementParser statementParser;

    /**
     * Creates a middleware instance with explicit component wiring.
     *
     * @param engine         decision engine used for validate/rewrite/render decisions
     * @param explainer      explainer used by {@link #explainDecision(String, ExecutionContext)}
     * @param auditPublisher audit sink for emitted audit events
     * @param guardrails     runtime guardrail settings
     * @param statementParser parser used for ingress SQL parsing
     */
    public DefaultSqlDecisionService(
        SqlDecisionEngine engine,
        SqlDecisionExplainer explainer,
        AuditEventPublisher auditPublisher,
        RuntimeGuardrails guardrails,
        SqlStatementParser statementParser
    ) {
        this.engine = engine;
        this.explainer = explainer;
        this.auditPublisher = auditPublisher;
        this.guardrails = guardrails;
        this.statementParser = statementParser;
    }

    private static Long extractLimit(Statement statement) {
        return switch (statement) {
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
        var explanation = evaluated.statement() == null
            ? null
            : explainer.explain(evaluated.statement(), context.withExecutionMode(ExecutionMode.ANALYZE), decision);
        if (explanation == null || explanation.isBlank()) {
            explanation = "Decision=%s, reason=%s".formatted(decision.kind(), decision.reasonCode());
        }
        return new DecisionExplanation(decision, explanation);
    }

    private EvaluatedDecision evaluate(String sql, ExecutionContext context, ExecutionMode mode) {
        validateInput(sql, context);
        var contextWithMode = context.withExecutionMode(mode);
        var startedNanos = System.nanoTime();
        Node statement = null;

        var decision = precheckSqlLength(sql);
        if (decision == null) {
            try {
                statement = statementParser.parse(sql, contextWithMode);
            } catch (RuntimeException ex) {
                decision = DecisionResult.deny(ReasonCode.DENY_PIPELINE_ERROR, ex.getMessage());
            }
        }
        if (decision == null) {
            decision = evaluateParsed(statement, contextWithMode, sql, mode);
        }

        emitAuditEvent(sql, contextWithMode, decision, System.nanoTime() - startedNanos);
        return new EvaluatedDecision(decision, statement);
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

    private DecisionResult evaluateWithTimeout(Node statement, ExecutionContext contextWithMode) {
        if (guardrails.timeoutMillis() == null) {
            try {
                return Objects.requireNonNull(engine.evaluate(statement, contextWithMode), "engine must return a decision");
            } catch (RuntimeException ex) {
                return DecisionResult.deny(ReasonCode.DENY_PIPELINE_ERROR, ex.getMessage());
            }
        }

        try (var executor = Executors.newSingleThreadExecutor()) {
            var future = executor.submit(() -> Objects.requireNonNull(engine.evaluate(statement, contextWithMode), "engine must return a decision"));
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

    private DecisionResult applyMaxRowsGuardrail(Node statement, DecisionResult decision) {
        if (guardrails.maxRows() == null || decision.kind() == DecisionKind.DENY) {
            return decision;
        }

        if (statement instanceof StatementSequence sequence) {
            for (int i = 0; i < sequence.statements().size(); i++) {
                var result = applyMaxRowsGuardrail(sequence.statements().get(i), decision);
                if (result.kind() == DecisionKind.DENY) {
                    return DecisionResult.deny(
                        result.reasonCode(),
                        "Statement %d: %s".formatted(i + 1, result.message())
                    );
                }
            }
            return decision;
        }

        if (statement instanceof Statement singleStatement) {
            return applyMaxRowsGuardrail(singleStatement, decision);
        }
        return DecisionResult.deny(
            ReasonCode.DENY_PIPELINE_ERROR,
            "Unsupported statement model: " + statement.getClass().getName()
        );
    }

    private DecisionResult applyMaxStatementsGuardrail(Node statement) {
        if (guardrails.maxStatementsPerRequest() == null) {
            return null;
        }
        var statementCount = statement instanceof StatementSequence sequence ? sequence.statements().size() : 1;
        if (statementCount <= guardrails.maxStatementsPerRequest()) {
            return null;
        }
        return DecisionResult.deny(
            ReasonCode.DENY_MAX_STATEMENTS,
            "Statement count %d exceeds configured maxStatementsPerRequest %d".formatted(
                statementCount,
                guardrails.maxStatementsPerRequest()
            )
        );
    }

    private DecisionResult evaluateParsed(Node statement, ExecutionContext contextWithMode, String sql, ExecutionMode mode) {
        var decision = applyMaxStatementsGuardrail(statement);
        if (decision == null) {
            decision = evaluateWithTimeout(statement, contextWithMode);
            decision = applyMaxRowsGuardrail(statement, decision);
            decision = applyExplainDryRun(sql, mode, decision);
        }
        return decision;
    }

    private DecisionResult applyMaxRowsGuardrail(Statement statement, DecisionResult decision) {
        var detectedLimit = extractLimit(statement);
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

    private record EvaluatedDecision(DecisionResult decision, Node statement) {
    }
}




