package io.sqm.transpile.rule;

import io.sqm.core.dialect.SqlDialectId;
import io.sqm.transpile.builtin.MySqlToPostgresInsertModeUnsupportedRule;
import io.sqm.transpile.builtin.MySqlToPostgresJsonFunctionUnsupportedRule;
import io.sqm.transpile.builtin.MySqlToPostgresNullSafeComparisonRule;
import io.sqm.transpile.builtin.MySqlToPostgresOnDuplicateKeyUnsupportedRule;
import io.sqm.transpile.builtin.MySqlToPostgresHintDroppingRule;
import io.sqm.transpile.builtin.PostgresToMySqlIlikeRule;
import io.sqm.transpile.builtin.PostgresToMySqlNullSafeComparisonRule;
import io.sqm.transpile.builtin.PostgresToMySqlDistinctOnUnsupportedRule;
import io.sqm.transpile.builtin.PostgresToMySqlOperatorFamilyUnsupportedRule;
import io.sqm.transpile.builtin.PostgresToMySqlRegexVariantUnsupportedRule;
import io.sqm.transpile.builtin.PostgresToMySqlReturningUnsupportedRule;
import io.sqm.transpile.builtin.PostgresToMySqlSimilarToUnsupportedRule;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Default reusable rule registry implementation.
 */
public final class DefaultTranspileRuleRegistry implements TranspileRuleRegistry {
    private final List<TranspileRule> rules;

    private DefaultTranspileRuleRegistry(List<TranspileRule> rules) {
        this.rules = List.copyOf(rules);
    }

    /**
     * Returns the default built-in registry.
     *
     * @return default rule registry
     */
    public static DefaultTranspileRuleRegistry defaults() {
        return new DefaultTranspileRuleRegistry(List.of(
            new PostgresToMySqlNullSafeComparisonRule(),
            new PostgresToMySqlIlikeRule(),
            new PostgresToMySqlReturningUnsupportedRule(),
            new PostgresToMySqlDistinctOnUnsupportedRule(),
            new PostgresToMySqlSimilarToUnsupportedRule(),
            new PostgresToMySqlRegexVariantUnsupportedRule(),
            new PostgresToMySqlOperatorFamilyUnsupportedRule(),
            new MySqlToPostgresNullSafeComparisonRule(),
            new MySqlToPostgresHintDroppingRule(),
            new MySqlToPostgresOnDuplicateKeyUnsupportedRule(),
            new MySqlToPostgresInsertModeUnsupportedRule(),
            new MySqlToPostgresJsonFunctionUnsupportedRule()
        ));
    }

    /**
     * Returns a registry backed by the provided rules.
     *
     * @param rules transpilation rules
     * @return rule registry
     */
    public static DefaultTranspileRuleRegistry of(List<TranspileRule> rules) {
        Objects.requireNonNull(rules, "rules");
        return new DefaultTranspileRuleRegistry(rules);
    }

    /**
     * Returns a registry backed by the provided rules.
     *
     * @param rules transpilation rules
     * @return rule registry
     */
    public static DefaultTranspileRuleRegistry of(TranspileRule... rules) {
        return of(List.of(rules));
    }

    @Override
    public List<TranspileRule> rulesFor(SqlDialectId source, SqlDialectId target) {
        var selected = new ArrayList<TranspileRule>();
        for (var rule : rules) {
            if (rule.supports(source, target)) {
                selected.add(rule);
            }
        }
        selected.sort(Comparator.comparingInt(TranspileRule::order).thenComparing(TranspileRule::id));
        return List.copyOf(selected);
    }
}
