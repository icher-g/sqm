package io.sqm.control;

import io.sqm.control.impl.DefaultSqlDecisionService;

import java.util.Objects;

/**
 * Framework entry points for middleware SQL analysis and enforcement flows.
 */
public interface SqlDecisionService {
    /**
     * Creates middleware from a named configuration object.
     *
     * @param config middleware configuration
     * @return middleware instance
     */
    static SqlDecisionService create(SqlDecisionServiceConfig config) {
        Objects.requireNonNull(config, "config must not be null");
        return new DefaultSqlDecisionService(
            config.engine(),
            config.explainer(),
            config.auditPublisher(),
            config.guardrails(),
            config.queryParser()
        );
    }

    /**
     * Evaluates SQL in analyze mode.
     *
     * @param sql     input SQL
     * @param context execution context
     * @return decision result
     */
    DecisionResult analyze(String sql, ExecutionContext context);

    /**
     * Evaluates SQL in enforce (execute-intent) mode.
     *
     * <p>This method does not execute SQL. It returns a decision and may rewrite the SQL
     * (for example, {@code EXPLAIN} dry-run when enabled by {@link RuntimeGuardrails}).</p>
     *
     * @param sql     input SQL
     * @param context execution context
     * @return decision result for execute-intent flow
     */
    DecisionResult enforce(String sql, ExecutionContext context);

    /**
     * Evaluates SQL in analyze mode and returns decision with explanation.
     *
     * @param sql     input SQL
     * @param context execution context
     * @return decision with explanation
     */
    DecisionExplanation explainDecision(String sql, ExecutionContext context);
}

