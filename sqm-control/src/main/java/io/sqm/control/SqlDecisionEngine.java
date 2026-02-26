package io.sqm.control;

import io.sqm.core.Query;
import io.sqm.core.transform.QueryFingerprint;

import java.util.Objects;

/**
 * Functional contract for query-model decision evaluation.
 */
@FunctionalInterface
public interface SqlDecisionEngine {
    /**
     * Creates a full-flow decision engine with explicit pipeline wiring.
     *
     * @param queryValidator semantic validator
     * @param queryRewriter  query rewrite pipeline
     * @param queryRenderer  query renderer
     * @return decision engine
     */
    static SqlDecisionEngine of(SqlQueryValidator queryValidator, SqlQueryRewriter queryRewriter, SqlQueryRenderer queryRenderer) {
        Objects.requireNonNull(queryValidator, "queryValidator must not be null");
        Objects.requireNonNull(queryRewriter, "queryRewriter must not be null");
        Objects.requireNonNull(queryRenderer, "queryRenderer must not be null");
        return new Impl(queryValidator, queryRewriter, queryRenderer);
    }

    /**
     * Evaluates query model for the provided context and returns a decision.
     *
     * @param query   parsed query model
     * @param context execution context
     * @return decision result
     */
    DecisionResult evaluate(Query query, ExecutionContext context);

    /**
     * Default composed implementation of {@link SqlDecisionEngine}.
     *
     * @param queryValidator query validator used before and after rewrites
     * @param queryRewriter query rewrite pipeline
     * @param queryRenderer query renderer for rewritten output
     */
    record Impl(SqlQueryValidator queryValidator, SqlQueryRewriter queryRewriter, SqlQueryRenderer queryRenderer) implements SqlDecisionEngine {

        /**
         * Validates required engine collaborators.
         *
         * @param queryValidator query validator
         * @param queryRewriter  query rewriter
         * @param queryRenderer  query renderer
         */
        public Impl {
            Objects.requireNonNull(queryValidator, "queryValidator must not be null");
            Objects.requireNonNull(queryRewriter, "queryRewriter must not be null");
            Objects.requireNonNull(queryRenderer, "queryRenderer must not be null");
        }

        /**
         * Evaluates query model for the provided context and returns a decision.
         *
         * @param query   parsed query model
         * @param context execution context
         * @return decision result
         */
        @Override
        public DecisionResult evaluate(Query query, ExecutionContext context) {
            Objects.requireNonNull(query, "query must not be null");
            Objects.requireNonNull(context, "context must not be null");

            var validation = queryValidator.validate(query, context);
            if (validation.isFailed()) {
                return DecisionResult.deny(validation.code(), validation.message());
            }

            final QueryRewriteResult rewrite;

            try {
                rewrite = queryRewriter.rewrite(query, context);
            } catch (RewriteDenyException ex) {
                return DecisionResult.deny(ex.reasonCode(), ex.getMessage());
            }

            if (!rewrite.rewritten()) {
                return DecisionResult.allow(QueryFingerprint.of(query));
            }

            var rewrittenValidation = queryValidator.validate(rewrite.query(), context);
            if (rewrittenValidation.isFailed()) {
                return DecisionResult.deny(rewrittenValidation.code(), rewrittenValidation.message());
            }

            QueryRenderResult renderResult = queryRenderer.render(rewrite.query(), context);
            Objects.requireNonNull(renderResult.sql(), "queryRenderer must not return null");

            String message = "Rewritten by policy rules: " + String.join(", ", rewrite.appliedRuleIds());

            return DecisionResult.rewrite(
                rewrite.primaryReasonCode(),
                message,
                renderResult.sql(),
                renderResult.params(),
                QueryFingerprint.of(rewrite.query())
            );
        }
    }
}
