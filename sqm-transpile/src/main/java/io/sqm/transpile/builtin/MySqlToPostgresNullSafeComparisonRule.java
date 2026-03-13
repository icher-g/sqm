package io.sqm.transpile.builtin;

import io.sqm.core.*;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.core.transform.RecursiveNodeTransformer;
import io.sqm.transpile.RewriteFidelity;
import io.sqm.transpile.TranspileContext;
import io.sqm.transpile.TranspileRuleResult;
import io.sqm.transpile.rule.TranspileRule;

import java.util.Set;

/**
 * Rewrites MySQL null-safe equality syntax into PostgreSQL distinctness predicates.
 */
public final class MySqlToPostgresNullSafeComparisonRule implements TranspileRule {
    /**
     * Creates a MySQL-to-PostgreSQL null-safe comparison rewrite rule.
     */
    public MySqlToPostgresNullSafeComparisonRule() {
    }

    @Override
    public String id() {
        return "mysql-to-postgres-null-safe-comparison";
    }

    @Override
    public Set<SqlDialectId> sourceDialects() {
        return Set.of(SqlDialectId.MYSQL);
    }

    @Override
    public Set<SqlDialectId> targetDialects() {
        return Set.of(SqlDialectId.POSTGRESQL);
    }

    @Override
    public TranspileRuleResult apply(Statement statement, TranspileContext context) {
        var transformer = new NullSafeComparisonTransformer();
        var rewritten = transformer.transform(statement);
        if (rewritten == statement) {
            return TranspileRuleResult.unchanged(statement, "No MySQL null-safe comparison rewrite needed");
        }
        return TranspileRuleResult.rewritten(
            rewritten,
            RewriteFidelity.EXACT,
            "Lowered MySQL null-safe equality to PostgreSQL distinctness predicates"
        );
    }

    private static final class NullSafeComparisonTransformer extends RecursiveNodeTransformer {
        private Statement transform(Statement statement) {
            return apply(statement);
        }

        @Override
        public io.sqm.core.Node visitComparisonPredicate(ComparisonPredicate predicate) {
            if (predicate.operator() != ComparisonOperator.NULL_SAFE_EQ) {
                return super.visitComparisonPredicate(predicate);
            }
            var lhs = apply(predicate.lhs());
            var rhs = apply(predicate.rhs());
            return IsDistinctFromPredicate.of(lhs, rhs, true);
        }

        @Override
        public io.sqm.core.Node visitNotPredicate(NotPredicate predicate) {
            if (predicate.inner() instanceof ComparisonPredicate comparison
                && comparison.operator() == ComparisonOperator.NULL_SAFE_EQ) {
                var lhs = apply(comparison.lhs());
                var rhs = apply(comparison.rhs());
                return IsDistinctFromPredicate.of(lhs, rhs, false);
            }
            return super.visitNotPredicate(predicate);
        }
    }
}
