package io.sqm.control;

import io.sqm.core.Query;

/**
 * Functional contract for query-model decision evaluation.
 */
@FunctionalInterface
public interface SqlDecisionEngine {
    /**
     * Evaluates query model for the provided context and returns a decision.
     *
     * @param query   parsed query model
     * @param context execution context
     * @return decision result
     */
    DecisionResult evaluate(Query query, ExecutionContext context);
}
