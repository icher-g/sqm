package io.sqm.transpile.builtin;

import io.sqm.core.LimitOffset;
import io.sqm.core.SelectQuery;
import io.sqm.core.SelectQueryBuilder;
import io.sqm.core.Statement;
import io.sqm.core.TopSpec;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.core.transform.RecursiveNodeTransformer;
import io.sqm.core.walk.RecursiveNodeVisitor;
import io.sqm.transpile.RewriteFidelity;
import io.sqm.transpile.TranspileContext;
import io.sqm.transpile.TranspileRuleResult;
import io.sqm.transpile.rule.TranspileRule;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Rewrites baseline SQL Server {@code TOP} queries into generic limit-only
 * SELECT queries for non-SQL Server targets.
 */
public final class SqlServerTopToLimitRule implements TranspileRule {
    /**
     * Creates a TOP-to-limit rewrite rule.
     */
    public SqlServerTopToLimitRule() {
    }

    @Override
    public String id() {
        return "sqlserver-top-to-limit";
    }

    @Override
    public Set<SqlDialectId> sourceDialects() {
        return Set.of(SqlDialectId.SQLSERVER);
    }

    @Override
    public Set<SqlDialectId> targetDialects() {
        return Set.of(SqlDialectId.ANSI, SqlDialectId.MYSQL, SqlDialectId.POSTGRESQL);
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    public TranspileRuleResult apply(Statement statement, TranspileContext context) {
        var unsupportedVariant = unsupportedTopVariant(statement);
        if (unsupportedVariant != null) {
            return TranspileRuleResult.unsupported(
                statement,
                unsupportedVariant.code(),
                unsupportedVariant.message()
            );
        }

        var transformer = new TopToLimitTransformer();
        var rewritten = transformer.transformStatement(statement);
        if (rewritten == statement) {
            return TranspileRuleResult.unchanged(statement, "No SQL Server TOP usage detected");
        }
        return TranspileRuleResult.rewritten(
            rewritten,
            RewriteFidelity.EXACT,
            "Rewrote SQL Server TOP to generic limit-only SELECT queries"
        );
    }

    private static UnsupportedTopVariant unsupportedTopVariant(Statement statement) {
        var found = new AtomicReference<UnsupportedTopVariant>();
        statement.accept(new RecursiveNodeVisitor<Void>() {
            @Override
            protected Void defaultResult() {
                return null;
            }

            @Override
            public Void visitTopSpec(TopSpec spec) {
                if (found.get() == null) {
                    if (spec.percent()) {
                        found.set(new UnsupportedTopVariant(
                            "UNSUPPORTED_SQLSERVER_TOP_PERCENT",
                            "SQL Server TOP PERCENT cannot be transpiled exactly to LIMIT/OFFSET-style targets"
                        ));
                    } else if (spec.withTies()) {
                        found.set(new UnsupportedTopVariant(
                            "UNSUPPORTED_SQLSERVER_TOP_WITH_TIES",
                            "SQL Server TOP WITH TIES cannot be transpiled exactly to LIMIT/OFFSET-style targets"
                        ));
                    }
                }
                return super.visitTopSpec(spec);
            }
        });
        return found.get();
    }

    private record UnsupportedTopVariant(String code, String message) {
    }

    private static final class TopToLimitTransformer extends RecursiveNodeTransformer {
        private Statement transformStatement(Statement statement) {
            return apply(statement);
        }

        @Override
        public io.sqm.core.Node visitSelectQuery(SelectQuery query) {
            var transformed = (SelectQuery) super.visitSelectQuery(query);
            TopSpec topSpec = transformed.topSpec();
            if (topSpec == null) {
                return transformed;
            }
            return SelectQueryBuilder.of(transformed)
                .top((TopSpec) null)
                .limitOffset(LimitOffset.of(topSpec.count(), null))
                .build();
        }
    }
}
