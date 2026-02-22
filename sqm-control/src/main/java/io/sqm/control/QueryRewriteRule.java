package io.sqm.control;

import io.sqm.core.Query;

/**
 * Applies a deterministic policy rewrite to a parsed SQM {@link Query}.
 */
@FunctionalInterface
public interface QueryRewriteRule {

    /**
     * Returns a stable identifier for this rule.
     *
     * @return rule identifier
     */
    default String id() {
        return getClass().getSimpleName();
    }

    /**
     * Applies the rule to the provided query model.
     *
     * @param query   parsed query model
     * @param context execution context
     * @return rewrite result
     */
    QueryRewriteResult apply(Query query, ExecutionContext context);
}
