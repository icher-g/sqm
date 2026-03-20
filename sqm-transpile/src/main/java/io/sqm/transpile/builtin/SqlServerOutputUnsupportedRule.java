package io.sqm.transpile.builtin;

import io.sqm.core.Statement;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.transpile.TranspileContext;
import io.sqm.transpile.TranspileRuleResult;
import io.sqm.transpile.rule.TranspileRule;

import java.util.Set;

/**
 * Rejects SQL Server DML {@code OUTPUT} clauses when targeting non-SQL Server dialects.
 */
public final class SqlServerOutputUnsupportedRule implements TranspileRule {

    /**
     * Creates a SQL Server output rejection rule.
     */
    public SqlServerOutputUnsupportedRule() {
    }

    @Override
    public String id() {
        return "sqlserver-output-unsupported";
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
        return 90;
    }

    @Override
    public TranspileRuleResult apply(Statement statement, TranspileContext context) {
        if (!StatementFeatureInspector.hasResultClause(statement)) {
            return TranspileRuleResult.unchanged(statement, "No SQL Server OUTPUT usage detected");
        }

        return TranspileRuleResult.unsupported(
            statement,
            "UNSUPPORTED_SQLSERVER_OUTPUT",
            "SQL Server OUTPUT clauses cannot be transpiled exactly to non-SQL Server targets"
        );
    }
}
