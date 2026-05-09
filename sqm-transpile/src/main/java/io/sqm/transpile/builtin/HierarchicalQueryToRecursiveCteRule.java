package io.sqm.transpile.builtin;

import io.sqm.core.SelectQuery;
import io.sqm.core.Statement;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.core.transform.HierarchicalQueryToRecursiveCteTransformer;
import io.sqm.transpile.RewriteFidelity;
import io.sqm.transpile.TranspileContext;
import io.sqm.transpile.TranspileRuleResult;
import io.sqm.transpile.rule.TranspileRule;

import java.util.Set;

/**
 * Rewrites simple hierarchical queries into recursive common table expressions.
 */
public final class HierarchicalQueryToRecursiveCteRule implements TranspileRule {
    /**
     * Creates a hierarchical-query-to-recursive-CTE rewrite rule.
     */
    public HierarchicalQueryToRecursiveCteRule() {
    }

    /**
     * Returns the stable rule identifier.
     *
     * @return rule identifier
     */
    @Override
    public String id() {
        return "hierarchical-query-to-recursive-cte";
    }

    /**
     * Returns source dialects this rule applies to.
     *
     * @return Oracle source dialect
     */
    @Override
    public Set<SqlDialectId> sourceDialects() {
        return Set.of(SqlDialectId.ORACLE);
    }

    /**
     * Returns target dialects that use {@code WITH RECURSIVE}.
     *
     * @return recursive-CTE target dialects
     */
    @Override
    public Set<SqlDialectId> targetDialects() {
        return Set.of(SqlDialectId.ANSI, SqlDialectId.MYSQL, SqlDialectId.POSTGRESQL);
    }

    /**
     * Runs before generic unsupported-feature checks.
     *
     * @return rule execution order
     */
    @Override
    public int order() {
        return -10;
    }

    /**
     * Applies the hierarchical-query rewrite when the input shape can be represented by a recursive CTE.
     *
     * @param statement statement to rewrite
     * @param context transpilation context
     * @return rewrite result
     */
    @Override
    public TranspileRuleResult apply(Statement statement, TranspileContext context) {
        if (!StatementFeatureInspector.hasHierarchicalQuery(statement)) {
            return TranspileRuleResult.unchanged(statement, "No hierarchical query detected");
        }
        if (!(statement instanceof SelectQuery query)) {
            return unsupported(statement, "Only top-level SELECT hierarchical queries can be rewritten");
        }

        var hierarchical = query.hierarchical();
        if (hierarchical == null) {
            return TranspileRuleResult.unchanged(statement, "No hierarchical query detected");
        }

        try {
            var rewritten = new HierarchicalQueryToRecursiveCteTransformer().rewrite(query);
            return TranspileRuleResult.rewritten(rewritten, RewriteFidelity.EXACT, "Rewrote hierarchical query to recursive CTE");
        } catch (HierarchicalQueryToRecursiveCteTransformer.UnsupportedRewriteException ex) {
            return unsupported(statement, ex.getMessage());
        }
    }

    private static TranspileRuleResult unsupported(Statement statement, String message) {
        return TranspileRuleResult.unsupported(statement, "UNSUPPORTED_HIERARCHICAL_QUERY_REWRITE", message);
    }
}
