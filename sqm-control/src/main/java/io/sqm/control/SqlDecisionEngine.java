package io.sqm.control;

/**
 * Functional contract for SQL decision evaluation.
 */
@FunctionalInterface
public interface SqlDecisionEngine {
    /**
     * Evaluates SQL for the provided context and returns a decision.
     *
     * @param sql     input SQL
     * @param context execution context
     * @return decision result
     */
    DecisionResult evaluate(String sql, ExecutionContext context);
}
