package io.sqm.control.service;

import io.sqm.control.decision.DecisionResult;
import io.sqm.control.execution.ExecutionContext;
import io.sqm.core.Statement;

/**
 * Functional contract for building decision explanations.
 */
@FunctionalInterface
public interface SqlDecisionExplainer {
    /**
     * Creates a default explainer that uses the decision message when available.
     *
     * @return default explainer
     */
    static SqlDecisionExplainer basic() {
        return (query, context, decision) -> {
            if (decision.message() != null && !decision.message().isBlank()) {
                return decision.message();
            }
            return "Decision=%s, reason=%s".formatted(decision.kind(), decision.reasonCode());
        };
    }

    /**
     * Builds an explanation for a decision.
     *
     * @param query    parsed statement model
     * @param context  execution context
     * @param decision decision result
     * @return explanation text
     */
    String explain(Statement query, ExecutionContext context, DecisionResult decision);
}



