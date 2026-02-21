package io.sqm.control;

import java.util.Objects;

/**
 * Framework entry points for middleware SQL analysis and enforcement flows.
 */
public interface SqlMiddleware {
    /**
     * Creates middleware using the provided decision engine and the default explainer.
     *
     * @param engine decision engine
     * @return middleware instance
     */
    static SqlMiddleware of(SqlDecisionEngine engine) {
        return of(engine, SqlDecisionExplainer.basic());
    }

    /**
     * Creates middleware using the provided decision engine and explanation strategy.
     *
     * @param engine    decision engine
     * @param explainer explanation strategy
     * @return middleware instance
     */
    static SqlMiddleware of(SqlDecisionEngine engine, SqlDecisionExplainer explainer) {
        Objects.requireNonNull(engine, "engine must not be null");
        Objects.requireNonNull(explainer, "explainer must not be null");
        return new SqlMiddlewareImpl(engine, explainer);
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
     * @param sql     input SQL
     * @param context execution context
     * @return decision result
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
