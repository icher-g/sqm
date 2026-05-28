package io.sqm.transpile.rule;

import io.sqm.core.dialect.SqlDialectId;
import io.sqm.transpile.builtin.*;

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
            new FunctionTableToMySqlUnsupportedRule(),
            new PostgresToMySqlReturningUnsupportedRule(),
            new PostgresToMySqlDistinctOnUnsupportedRule(),
            new PostgresToMySqlSimilarToUnsupportedRule(),
            new PostgresToMySqlRegexVariantUnsupportedRule(),
            new PostgresToMySqlOperatorFamilyUnsupportedRule(),
            new PostgresMergeUnsupportedRule(),
            new PostgresMergeDoNothingUnsupportedRule(),
            new PostgresMergeNotMatchedBySourceToOracleUnsupportedRule(),
            new PostgresToOracleDistinctOnUnsupportedRule(),
            new MySqlToPostgresNullSafeComparisonRule(),
            new MySqlHintDroppingRule(),
            new MySqlToPostgresOnDuplicateKeyUnsupportedRule(),
            new MySqlToPostgresInsertModeUnsupportedRule(),
            new MySqlToPostgresJsonFunctionUnsupportedRule(),
            new OracleResultClauseUnsupportedRule(),
            new OracleReturningIntoUnsupportedRule(),
            new OracleHintDroppingRule(),
            new StandardLimitToSqlServerTopRule(),
            new SqlServerTopToLimitRule(),
            new SqlServerOutputUnsupportedRule(),
            new SqlServerMergeUnsupportedRule(),
            new SqlServerHintDroppingRule(),
            new PostgresToSqlServerDistinctOnUnsupportedRule(),
            new PostgresToSqlServerReturningUnsupportedRule(),
            new SequenceValueUnsupportedRule(),
            new TableAccessModifierUnsupportedRule(),
            new PivotUnpivotApproximateRewriteRule(),
            new HierarchicalQueryToRecursiveCteRule(),
            new HierarchicalQueryUnsupportedRule(),
            new OracleToSqlServerPivotUnpivotRule()
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
