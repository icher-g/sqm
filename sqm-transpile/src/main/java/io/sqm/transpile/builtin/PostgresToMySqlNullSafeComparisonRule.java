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
 * Rewrites PostgreSQL distinctness predicates into MySQL null-safe equality syntax.
 */
public final class PostgresToMySqlNullSafeComparisonRule implements TranspileRule {
    /**
     * Creates a PostgreSQL-to-MySQL null-safe comparison rewrite rule.
     */
    public PostgresToMySqlNullSafeComparisonRule() {
    }

    @Override
    public String id() {
        return "postgres-to-mysql-null-safe-comparison";
    }

    @Override
    public Set<SqlDialectId> sourceDialects() {
        return Set.of(SqlDialectId.POSTGRESQL);
    }

    @Override
    public Set<SqlDialectId> targetDialects() {
        return Set.of(SqlDialectId.MYSQL);
    }

    @Override
    public TranspileRuleResult apply(Statement statement, TranspileContext context) {
        var transformer = new DistinctnessLoweringTransformer();
        var rewritten = transformer.transform(statement);
        if (rewritten == statement) {
            return TranspileRuleResult.unchanged(statement, "No distinctness predicate rewrite needed");
        }
        return TranspileRuleResult.rewritten(
            rewritten,
            RewriteFidelity.EXACT,
            "Lowered PostgreSQL distinctness predicates to MySQL null-safe equality"
        );
    }

    private static final class DistinctnessLoweringTransformer extends RecursiveNodeTransformer {
        private Statement transform(Statement statement) {
            return apply(statement);
        }

        @Override
        public Node visitIsDistinctFromPredicate(IsDistinctFromPredicate predicate) {
            var lhs = apply(predicate.lhs());
            var rhs = apply(predicate.rhs());
            var nullSafeEq = ComparisonPredicate.of(lhs, ComparisonOperator.NULL_SAFE_EQ, rhs);
            if (predicate.negated()) {
                return nullSafeEq;
            }
            return NotPredicate.of(nullSafeEq);
        }
    }
}
