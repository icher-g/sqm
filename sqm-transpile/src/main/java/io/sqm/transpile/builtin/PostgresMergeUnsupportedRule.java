package io.sqm.transpile.builtin;

import io.sqm.core.Statement;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.transpile.TranspileContext;
import io.sqm.transpile.TranspileRuleResult;
import io.sqm.transpile.rule.TranspileRule;

import java.util.Set;

/**
 * Rejects PostgreSQL MERGE statements for non-PostgreSQL targets.
 */
public final class PostgresMergeUnsupportedRule implements TranspileRule {

    /**
     * Creates a PostgreSQL MERGE rejection rule.
     */
    public PostgresMergeUnsupportedRule() {
    }

    @Override
    public String id() {
        return "postgres-merge-unsupported";
    }

    @Override
    public Set<SqlDialectId> sourceDialects() {
        return Set.of(SqlDialectId.POSTGRESQL);
    }

    @Override
    public Set<SqlDialectId> targetDialects() {
        return Set.of(SqlDialectId.ANSI, SqlDialectId.MYSQL, SqlDialectId.SQLSERVER);
    }

    @Override
    public int order() {
        return 100;
    }

    @Override
    public TranspileRuleResult apply(Statement statement, TranspileContext context) {
        if (!StatementFeatureInspector.hasMergeStatement(statement)) {
            return TranspileRuleResult.unchanged(statement, "No PostgreSQL MERGE usage detected");
        }

        return TranspileRuleResult.unsupported(
            statement,
            "UNSUPPORTED_POSTGRES_MERGE",
            "PostgreSQL MERGE cannot be transpiled exactly to non-PostgreSQL targets"
        );
    }
}
