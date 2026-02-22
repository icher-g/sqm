package io.sqm.control;

import io.sqm.core.Query;
import io.sqm.control.rewrite.BuiltInSqlRewriters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Composes and applies query rewrite rules before final decision rendering.
 */
@FunctionalInterface
public interface SqlQueryRewriter {
    /**
     * Returns a rewriter composed from all currently available built-in rewrite rules.
     *
     * @return built-in rewrite pipeline, or no-op when no built-ins are available yet
     */
    static SqlQueryRewriter allBuiltIn() {
        return BuiltInSqlRewriters.allAvailable();
    }

    /**
     * Returns a rewriter composed from selected built-in rewrite rules.
     *
     * @param rules built-in rewrite rules to enable
     * @return built-in rewrite pipeline
     */
    static SqlQueryRewriter builtIn(BuiltInRewriteRule... rules) {
        return BuiltInSqlRewriters.of(rules);
    }

    /**
     * Returns a rewriter composed from selected built-in rewrite rules.
     *
     * @param rules built-in rewrite rules to enable
     * @return built-in rewrite pipeline
     */
    static SqlQueryRewriter builtIn(Set<BuiltInRewriteRule> rules) {
        return BuiltInSqlRewriters.of(rules);
    }

    /**
     * Returns a rewriter that leaves queries unchanged.
     *
     * @return no-op rewriter
     */
    static SqlQueryRewriter noop() {
        return (query, context) -> QueryRewriteResult.unchanged(query);
    }

    /**
     * Returns a sequential rewriter that applies the provided rules in order.
     *
     * @param rules rewrite rules
     * @return composed rewriter
     */
    static SqlQueryRewriter chain(QueryRewriteRule... rules) {
        Objects.requireNonNull(rules, "rules must not be null");
        return chain(Arrays.asList(rules));
    }

    /**
     * Returns a sequential rewriter that applies the provided rules in order.
     *
     * @param rules rewrite rules
     * @return composed rewriter
     */
    static SqlQueryRewriter chain(List<? extends QueryRewriteRule> rules) {
        Objects.requireNonNull(rules, "rules must not be null");

        var copy = List.copyOf(rules);
        if (copy.isEmpty()) {
            return noop();
        }

        for (QueryRewriteRule rule : copy) {
            Objects.requireNonNull(rule, "rules must not contain null values");
        }

        return (query, context) -> {
            Objects.requireNonNull(query, "query must not be null");
            Objects.requireNonNull(context, "context must not be null");

            Query current = query;
            boolean rewritten = false;
            List<String> appliedRuleIds = new ArrayList<>();
            ReasonCode primaryReasonCode = ReasonCode.NONE;

            for (QueryRewriteRule rule : copy) {
                QueryRewriteResult result = Objects.requireNonNull(
                    rule.apply(current, context),
                    "rewrite rule must not return null"
                );
                current = result.query();
                if (result.rewritten() && !rewritten) {
                    primaryReasonCode = result.primaryReasonCode();
                }
                rewritten = rewritten || result.rewritten();
                appliedRuleIds.addAll(result.appliedRuleIds());
            }

            return rewritten
                ? new QueryRewriteResult(current, true, appliedRuleIds, primaryReasonCode)
                : QueryRewriteResult.unchanged(current);
        };
    }

    /**
     * Rewrites the provided query model for the given execution context.
     *
     * @param query   parsed query model
     * @param context execution context
     * @return rewrite result
     */
    QueryRewriteResult rewrite(Query query, ExecutionContext context);
}
