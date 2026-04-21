package io.sqm.control.pipeline;

import io.sqm.catalog.model.CatalogSchema;
import io.sqm.control.decision.ReasonCode;
import io.sqm.control.execution.ExecutionContext;
import io.sqm.control.rewrite.BuiltInRewriteRule;
import io.sqm.control.rewrite.BuiltInRewriteRules;
import io.sqm.control.rewrite.BuiltInRewriteSettings;
import io.sqm.core.Node;
import io.sqm.core.Statement;
import io.sqm.core.StatementSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Composes and applies statement rewrite rules before final decision rendering.
 */
@FunctionalInterface
public interface SqlStatementRewriter {
    /**
     * Creates a builder for composing a rewriter from built-in rule sources and optional schema/settings context.
     *
     * @return rewriter builder
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Returns a rewriter that leaves statements unchanged.
     *
     * @return no-op rewriter
     */
    static SqlStatementRewriter noop() {
        return (statement, context) -> StatementRewriteResult.unchanged(statement);
    }

    /**
     * Returns a sequential rewriter that applies the provided rules in order.
     *
     * @param rules rewrite rules
     * @return composed rewriter
     */
    static SqlStatementRewriter chain(StatementRewriteRule... rules) {
        Objects.requireNonNull(rules, "rules must not be null");
        var copy = List.of(rules);
        if (copy.isEmpty()) {
            return noop();
        }

        for (StatementRewriteRule rule : copy) {
            Objects.requireNonNull(rule, "rules must not contain null values");
        }

        return (statement, context) -> {
            Objects.requireNonNull(statement, "statement must not be null");
            Objects.requireNonNull(context, "context must not be null");

            Statement current = statement;
            boolean rewritten = false;
            List<String> appliedRuleIds = new ArrayList<>();
            ReasonCode primaryReasonCode = ReasonCode.NONE;

            for (StatementRewriteRule rule : copy) {
                StatementRewriteResult result = Objects.requireNonNull(
                    rule.apply(current, context),
                    "rewrite rule must not return null"
                );
                if (!(result.statement() instanceof Statement resultStatement)) {
                    throw new IllegalStateException("Rewrite rule must return a Statement");
                }
                current = resultStatement;
                if (result.rewritten() && !rewritten) {
                    primaryReasonCode = result.primaryReasonCode();
                }
                rewritten = rewritten || result.rewritten();
                appliedRuleIds.addAll(result.appliedRuleIds());
            }

            return rewritten
                ? new StatementRewriteResult(current, true, appliedRuleIds, primaryReasonCode)
                : StatementRewriteResult.unchanged(current);
        };
    }

    /**
     * Rewrites the provided statement or statement sequence model for the given execution context.
     *
     * @param statement parsed statement or statement-sequence model
     * @param context execution context
     * @return rewrite result
     */
    default StatementRewriteResult rewrite(Node statement, ExecutionContext context) {
        Objects.requireNonNull(statement, "statement must not be null");
        if (statement instanceof Statement singleStatement) {
            return rewrite(singleStatement, context);
        }
        if (statement instanceof StatementSequence sequence) {
            var rewrittenStatements = new ArrayList<Statement>(sequence.statements().size());
            var rewritten = false;
            var appliedRuleIds = new ArrayList<String>();
            var primaryReasonCode = ReasonCode.NONE;
            for (int i = 0; i < sequence.statements().size(); i++) {
                var result = rewrite(sequence.statements().get(i), context);
                if (!(result.statement() instanceof Statement resultStatement)) {
                    throw new IllegalStateException("Statement rewrite must return a Statement for statement " + (i + 1));
                }
                rewrittenStatements.add(resultStatement);
                if (result.rewritten() && !rewritten) {
                    primaryReasonCode = result.primaryReasonCode();
                }
                rewritten = rewritten || result.rewritten();
                appliedRuleIds.addAll(result.appliedRuleIds());
            }
            return rewritten
                ? new StatementRewriteResult(StatementSequence.of(rewrittenStatements), true, appliedRuleIds, primaryReasonCode)
                : StatementRewriteResult.unchanged(sequence);
        }
        throw new IllegalArgumentException("Unsupported statement model: " + statement.getClass().getName());
    }

    /**
     * Rewrites the provided statement model for the given execution context.
     *
     * @param statement parsed statement model
     * @param context execution context
     * @return rewrite result
     */
    StatementRewriteResult rewrite(Statement statement, ExecutionContext context);

    /**
     * Builder for creating {@link SqlStatementRewriter} instances from a single configuration point.
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
        public SqlStatementRewriter build() {
            List<StatementRewriteRule> statementRules;

            if (rules.isEmpty()) {
                statementRules = schema == null
                    ? BuiltInRewriteRules.allAvailable(settings)
                    : BuiltInRewriteRules.allAvailable(schema, settings);
            }
            else {
                statementRules = schema == null
                    ? BuiltInRewriteRules.selected(settings, rules)
                    : BuiltInRewriteRules.selected(schema, settings, rules);
            }

            return chain(statementRules.toArray(StatementRewriteRule[]::new));
        }
    }
}



