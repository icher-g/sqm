package io.sqm.control.service;

import io.sqm.control.decision.DecisionResult;
import io.sqm.control.execution.ExecutionContext;
import io.sqm.control.pipeline.*;
import io.sqm.core.Query;
import io.sqm.core.Statement;
import io.sqm.core.transform.QueryFingerprint;

import java.util.Objects;

/**
 * Functional contract for statement-model decision evaluation.
 */
@FunctionalInterface
public interface SqlDecisionEngine {
    /**
     * Creates a full-flow decision engine with explicit pipeline wiring.
     *
     * @param statementValidator semantic validator
     * @param statementRewriter statement rewrite pipeline
     * @param statementRenderer statement renderer
     * @return decision engine
     */
    static SqlDecisionEngine of(SqlStatementValidator statementValidator, SqlStatementRewriter statementRewriter, SqlStatementRenderer statementRenderer) {
        Objects.requireNonNull(statementValidator, "statementValidator must not be null");
        Objects.requireNonNull(statementRewriter, "statementRewriter must not be null");
        Objects.requireNonNull(statementRenderer, "statementRenderer must not be null");
        return new Impl(statementValidator, statementRewriter, statementRenderer);
    }

    /**
     * Evaluates statement model for the provided context and returns a decision.
     *
     * @param query   parsed statement model
     * @param context execution context
     * @return decision result
     */
    DecisionResult evaluate(Statement query, ExecutionContext context);

    /**
     * Default composed implementation of {@link SqlDecisionEngine}.
     *
     * @param statementValidator statement validator used before and after rewrites
     * @param statementRewriter statement rewrite pipeline
     * @param statementRenderer statement renderer for rewritten output
     */
    record Impl(SqlStatementValidator statementValidator, SqlStatementRewriter statementRewriter, SqlStatementRenderer statementRenderer) implements SqlDecisionEngine {

        /**
         * Validates required engine collaborators.
         *
         * @param statementValidator statement validator
         * @param statementRewriter statement rewriter
         * @param statementRenderer statement renderer
         */
        public Impl {
            Objects.requireNonNull(statementValidator, "statementValidator must not be null");
            Objects.requireNonNull(statementRewriter, "statementRewriter must not be null");
            Objects.requireNonNull(statementRenderer, "statementRenderer must not be null");
        }

        /**
         * Evaluates statement model for the provided context and returns a decision.
         *
         * @param query   parsed statement model
         * @param context execution context
         * @return decision result
         */
        @Override
        public DecisionResult evaluate(Statement query, ExecutionContext context) {
            Objects.requireNonNull(query, "query must not be null");
            Objects.requireNonNull(context, "context must not be null");

            var validation = statementValidator.validate(query, context);
            if (validation.isFailed()) {
                return DecisionResult.deny(validation.code(), validation.message());
            }

            final StatementRewriteResult rewrite;

            try {
                rewrite = statementRewriter.rewrite(query, context);
            } catch (RewriteDenyException ex) {
                return DecisionResult.deny(ex.reasonCode(), ex.getMessage());
            }

            if (!rewrite.rewritten()) {
                return query instanceof Query queryModel
                    ? DecisionResult.allow(QueryFingerprint.of(queryModel))
                    : DecisionResult.allow();
            }

            var rewrittenValidation = statementValidator.validate(rewrite.statement(), context);
            if (rewrittenValidation.isFailed()) {
                return DecisionResult.deny(rewrittenValidation.code(), rewrittenValidation.message());
            }

            StatementRenderResult renderResult = statementRenderer.render(rewrite.statement(), context);
            Objects.requireNonNull(renderResult.sql(), "statementRenderer must not return null");

            String message = "Rewritten by policy rules: " + String.join(", ", rewrite.appliedRuleIds());

            return DecisionResult.rewrite(
                rewrite.primaryReasonCode(),
                message,
                renderResult.sql(),
                renderResult.params(),
                rewrite.statement() instanceof Query rewrittenQuery ? QueryFingerprint.of(rewrittenQuery) : null
            );
        }
    }
}



