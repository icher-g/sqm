package io.sqm.control.pipeline;

import io.sqm.control.execution.ExecutionContext;
import io.sqm.core.Statement;

/**
 * Applies a deterministic policy rewrite to a parsed SQM {@link Statement}.
 */
@FunctionalInterface
public interface StatementRewriteRule {

    /**
     * Returns a stable identifier for this rule.
     *
     * @return rule identifier
     */
    default String id() {
        return getClass().getSimpleName();
    }

    /**
     * Applies the rule to the provided statement model.
     *
     * @param statement parsed statement model
     * @param context execution context
     * @return rewrite result
     */
    StatementRewriteResult apply(Statement statement, ExecutionContext context);
}



