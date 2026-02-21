package io.sqm.control;

import java.util.Objects;

final class SqlMiddlewareImpl implements SqlMiddleware {
    private final SqlDecisionEngine engine;
    private final SqlDecisionExplainer explainer;

    SqlMiddlewareImpl(SqlDecisionEngine engine, SqlDecisionExplainer explainer) {
        this.engine = engine;
        this.explainer = explainer;
    }

    @Override
    public DecisionResult analyze(String sql, ExecutionContext context) {
        return evaluate(sql, context, ExecutionMode.ANALYZE);
    }

    @Override
    public DecisionResult enforce(String sql, ExecutionContext context) {
        return evaluate(sql, context, ExecutionMode.EXECUTE);
    }

    @Override
    public DecisionExplanation explainDecision(String sql, ExecutionContext context) {
        var decision = analyze(sql, context);
        var explanation = explainer.explain(sql, context.withMode(ExecutionMode.ANALYZE), decision);
        if (explanation == null || explanation.isBlank()) {
            explanation = "Decision=%s, reason=%s".formatted(decision.kind(), decision.reasonCode());
        }
        return new DecisionExplanation(decision, explanation);
    }

    private DecisionResult evaluate(String sql, ExecutionContext context, ExecutionMode mode) {
        validateInput(sql, context);
        var contextWithMode = context.withMode(mode);
        try {
            var result = engine.evaluate(sql, contextWithMode);
            return Objects.requireNonNull(result, "engine must return a decision");
        } catch (RuntimeException ex) {
            return DecisionResult.deny(ReasonCode.DENY_PIPELINE_ERROR, ex.getMessage());
        }
    }

    private static void validateInput(String sql, ExecutionContext context) {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("sql must not be blank");
        }
        Objects.requireNonNull(context, "context must not be null");
    }
}
