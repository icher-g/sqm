package io.sqm.transpile.builtin;

import io.sqm.core.LimitOffset;
import io.sqm.core.SelectQuery;
import io.sqm.core.SelectQueryBuilder;
import io.sqm.core.Statement;
import io.sqm.core.TopSpec;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.core.transform.RecursiveNodeTransformer;
import io.sqm.transpile.RewriteFidelity;
import io.sqm.transpile.TranspileContext;
import io.sqm.transpile.TranspileRuleResult;
import io.sqm.transpile.rule.TranspileRule;

import java.util.Set;

/**
 * Rewrites generic limit-only SELECT queries into SQL Server {@code TOP}
 * queries when targeting SQL Server.
 */
public final class StandardLimitToSqlServerTopRule implements TranspileRule {
    /**
     * Creates a limit-to-TOP rewrite rule.
     */
    public StandardLimitToSqlServerTopRule() {
    }

    @Override
    public String id() {
        return "standard-limit-to-sqlserver-top";
    }

    @Override
    public Set<SqlDialectId> sourceDialects() {
        return Set.of(SqlDialectId.ANSI, SqlDialectId.MYSQL, SqlDialectId.POSTGRESQL);
    }

    @Override
    public Set<SqlDialectId> targetDialects() {
        return Set.of(SqlDialectId.SQLSERVER);
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    public TranspileRuleResult apply(Statement statement, TranspileContext context) {
        var transformer = new LimitToTopTransformer();
        var rewritten = transformer.transformStatement(statement);
        if (rewritten == statement) {
            return TranspileRuleResult.unchanged(statement, "No limit-only SELECT usage detected");
        }
        return TranspileRuleResult.rewritten(
            rewritten,
            RewriteFidelity.EXACT,
            "Rewrote limit-only SELECT queries to SQL Server TOP"
        );
    }

    private static final class LimitToTopTransformer extends RecursiveNodeTransformer {
        private Statement transformStatement(Statement statement) {
            return apply(statement);
        }

        @Override
        public io.sqm.core.Node visitSelectQuery(SelectQuery query) {
            var transformed = (SelectQuery) super.visitSelectQuery(query);
            LimitOffset limitOffset = transformed.limitOffset();
            if (transformed.topSpec() != null
                || limitOffset == null
                || limitOffset.limit() == null
                || limitOffset.offset() != null
                || limitOffset.limitAll()) {
                return transformed;
            }
            return SelectQueryBuilder.of(transformed)
                .top(TopSpec.of(limitOffset.limit()))
                .limitOffset(null)
                .build();
        }
    }
}
