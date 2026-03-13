package io.sqm.transpile.builtin;

import io.sqm.core.Expression;
import io.sqm.core.FunctionExpr;
import io.sqm.core.LikeMode;
import io.sqm.core.LikePredicate;
import io.sqm.core.QualifiedName;
import io.sqm.core.Statement;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.core.transform.RecursiveNodeTransformer;
import io.sqm.transpile.RewriteFidelity;
import io.sqm.transpile.TranspileContext;
import io.sqm.transpile.TranspileRuleResult;
import io.sqm.transpile.TranspileWarning;
import io.sqm.transpile.rule.TranspileRule;

import java.util.List;
import java.util.Set;

/**
 * Rewrites PostgreSQL {@code ILIKE} predicates into lowercased {@code LIKE} predicates for MySQL.
 */
public final class PostgresToMySqlIlikeRule implements TranspileRule {
    /**
     * Creates a PostgreSQL-to-MySQL {@code ILIKE} rewrite rule.
     */
    public PostgresToMySqlIlikeRule() {
    }

    @Override
    public String id() {
        return "postgres-to-mysql-ilike";
    }

    @Override
    public Set<SqlDialectId> sourceDialects() {
        return Set.of(SqlDialectId.of("postgresql"));
    }

    @Override
    public Set<SqlDialectId> targetDialects() {
        return Set.of(SqlDialectId.of("mysql"));
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    public TranspileRuleResult apply(Statement statement, TranspileContext context) {
        var transformer = new IlikeLoweringTransformer();
        var rewritten = transformer.transform(statement);
        if (rewritten == statement) {
            return TranspileRuleResult.unchanged(statement, "No ILIKE usage detected");
        }
        return new TranspileRuleResult(
            rewritten,
            true,
            RewriteFidelity.APPROXIMATE,
            List.of(new TranspileWarning(
                "APPROXIMATE_ILIKE_LOWERING",
                "ILIKE was lowered to LOWER(value) LIKE LOWER(pattern); collation and index behavior may differ"
            )),
            List.of(),
            "Lowered PostgreSQL ILIKE to MySQL-compatible LIKE"
        );
    }

    private static final class IlikeLoweringTransformer extends RecursiveNodeTransformer {
        private Statement transform(Statement statement) {
            return apply(statement);
        }

        @Override
        public io.sqm.core.Node visitLikePredicate(LikePredicate predicate) {
            if (predicate.mode() != LikeMode.ILIKE) {
                return super.visitLikePredicate(predicate);
            }
            var value = apply(predicate.value());
            var pattern = apply(predicate.pattern());
            var escape = apply(predicate.escape());
            return LikePredicate.of(
                LikeMode.LIKE,
                lower(value),
                lower(pattern),
                escape,
                predicate.negated()
            );
        }

        private static Expression lower(Expression expression) {
            return FunctionExpr.of(
                QualifiedName.of("LOWER"),
                List.of(Expression.funcArg(expression)),
                null,
                null,
                null,
                null
            );
        }
    }
}
