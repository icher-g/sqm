package io.sqm.transpile.builtin;

import io.sqm.core.Statement;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.transpile.TranspileContext;
import io.sqm.transpile.TranspileRuleResult;
import io.sqm.transpile.rule.TranspileRule;

import java.util.Set;

/**
 * Rejects SQL Server MERGE statements for non-SQL Server targets.
 */
public final class SqlServerMergeUnsupportedRule implements TranspileRule {

    /**
     * Creates a SQL Server MERGE rejection rule.
     */
    public SqlServerMergeUnsupportedRule() {
    }

    @Override
    public String id() {
        return "sqlserver-merge-unsupported";
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
        return 100;
    }

    @Override
    public TranspileRuleResult apply(Statement statement, TranspileContext context) {
        if (!StatementFeatureInspector.hasMergeStatement(statement)) {
            return TranspileRuleResult.unchanged(statement, "No SQL Server MERGE usage detected");
        }

        return TranspileRuleResult.unsupported(
            statement,
            "UNSUPPORTED_SQLSERVER_MERGE",
            "SQL Server MERGE cannot be transpiled exactly to non-SQL Server targets"
        );
    }
}
