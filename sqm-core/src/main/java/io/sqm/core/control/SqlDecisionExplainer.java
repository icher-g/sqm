package io.sqm.core.control;

/**
 * Functional contract for building decision explanations.
 */
@FunctionalInterface
public interface SqlDecisionExplainer {
    /**
     * Builds an explanation for a decision.
     *
     * @param sql      input SQL
     * @param context  execution context
     * @param decision decision result
     * @return explanation text
     */
    String explain(String sql, ExecutionContext context, DecisionResult decision);

    /**
     * Creates a default explainer that uses the decision message when available.
     *
     * @return default explainer
     */
    static SqlDecisionExplainer basic() {
        return (sql, context, decision) -> {
            if (decision.message() != null && !decision.message().isBlank()) {
                return decision.message();
            }
            return "Decision=%s, reason=%s".formatted(decision.kind(), decision.reasonCode());
        };
    }
}
