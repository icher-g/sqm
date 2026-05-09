package io.sqm.transpile.builtin;

import io.sqm.core.Statement;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.transpile.TranspileContext;
import io.sqm.transpile.TranspileRuleResult;
import io.sqm.transpile.rule.TranspileRule;

import java.util.Set;

/**
 * Rejects hierarchical queries when the target dialect cannot represent them exactly.
 */
public final class HierarchicalQueryUnsupportedRule implements TranspileRule {
    /**
     * Creates a hierarchical query rejection rule.
     */
    public HierarchicalQueryUnsupportedRule() {
    }

    /**
     * Returns the stable rule identifier.
     *
     * @return rule identifier
     */
    @Override
    public String id() {
        return "hierarchical-query-unsupported";
    }

    /**
     * Returns source dialects this rule applies to.
     *
     * @return empty set meaning all source dialects
     */
    @Override
    public Set<SqlDialectId> sourceDialects() {
        return Set.of();
    }

    /**
     * Returns target dialects that cannot currently render the recursive CTE rewrite.
     *
     * @return unsupported target dialects
     */
    @Override
    public Set<SqlDialectId> targetDialects() {
        return Set.of(SqlDialectId.SQLSERVER);
    }

    /**
     * Applies the unsupported-feature rule.
     *
     * @param statement statement to inspect
     * @param context transpilation context
     * @return transpilation result
     */
    @Override
    public TranspileRuleResult apply(Statement statement, TranspileContext context) {
        if (!StatementFeatureInspector.hasHierarchicalQuery(statement)) {
            return TranspileRuleResult.unchanged(statement, "No hierarchical query detected");
        }
        return TranspileRuleResult.unsupported(
            statement,
            "UNSUPPORTED_HIERARCHICAL_QUERY",
            "Hierarchical queries are not supported for " + context.targetDialect().value() + " transpilation"
        );
    }
}
