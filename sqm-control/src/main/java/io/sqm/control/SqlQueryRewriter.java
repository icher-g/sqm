package io.sqm.control;

import io.sqm.catalog.model.CatalogSchema;
import io.sqm.control.rewrite.BuiltInRewriteRules;
import io.sqm.core.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Composes and applies query rewrite rules before final decision rendering.
 */
@FunctionalInterface
public interface SqlQueryRewriter {
    /**
     * Creates a builder for composing a rewriter from built-in rule sources and optional schema/settings context.
     *
     * @return rewriter builder
     */
    static Builder builder() {
        return new Builder();
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
        var copy = List.of(rules);
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

    /**
     * Builder for creating {@link SqlQueryRewriter} instances from a single configuration point.
     *
     * <p>Built-in rules are sourced from {@link BuiltInRewriteRules} and then composed in-order
     * into a runnable rewriter.</p>
     */
    final class Builder {
        private CatalogSchema schema;
        private BuiltInRewriteSettings settings = BuiltInRewriteSettings.defaults();
        private Set<BuiltInRewriteRule> rules = Set.of();

        private Builder() {
        }

        /**
         * Sets schema for schema-aware built-in rules.
         *
         * @param schema catalog schema
         * @return builder instance
         */
        public Builder schema(CatalogSchema schema) {
            this.schema = Objects.requireNonNull(schema, "schema must not be null");
            return this;
        }

        /**
         * Sets built-in rewrite settings.
         *
         * @param settings built-in rewrite settings
         * @return builder instance
         */
        public Builder settings(BuiltInRewriteSettings settings) {
            this.settings = Objects.requireNonNull(settings, "settings must not be null");
            return this;
        }

        /**
         * Sets built-in rules to add to the baseline built-in pack.
         *
         * @param rules rules to include
         * @return builder instance
         */
        public Builder rules(BuiltInRewriteRule... rules) {
            this.rules = Set.of(rules);
            return this;
        }

        /**
         * Sets built-in rules to add to the baseline built-in pack.
         *
         * @param rules rules to include
         * @return builder instance
         */
        public Builder rules(Set<BuiltInRewriteRule> rules) {
            this.rules = Set.copyOf(Objects.requireNonNull(rules, "rules must not be null"));
            return this;
        }

        /**
         * Builds the rewriter from configured schema/settings/rules.
         *
         * @return configured rewriter
         */
        public SqlQueryRewriter build() {
            List<QueryRewriteRule> queryRules;

            if (rules.isEmpty()) {
                queryRules = schema == null
                    ? BuiltInRewriteRules.allAvailable(settings)
                    : BuiltInRewriteRules.allAvailable(schema, settings);
            }
            else {
                queryRules = schema == null
                    ? BuiltInRewriteRules.selected(settings, rules)
                    : BuiltInRewriteRules.selected(schema, settings, rules);
            }

            return chain(queryRules.toArray(QueryRewriteRule[]::new));
        }
    }
}
