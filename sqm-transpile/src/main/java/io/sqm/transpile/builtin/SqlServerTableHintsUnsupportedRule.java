package io.sqm.transpile.builtin;

import io.sqm.core.Statement;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.transpile.TranspileContext;
import io.sqm.transpile.TranspileRuleResult;
import io.sqm.transpile.rule.TranspileRule;

import java.util.Set;

/**
 * Rejects SQL Server table hints when targeting non-SQL Server dialects.
 */
public final class SqlServerTableHintsUnsupportedRule implements TranspileRule {

    /**
     * Creates a SQL Server table hint rejection rule.
     */
    public SqlServerTableHintsUnsupportedRule() {
    }

    @Override
    public String id() {
        return "sqlserver-table-hints-unsupported";
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
        if (!StatementFeatureInspector.hasLockHints(statement)) {
            return TranspileRuleResult.unchanged(statement, "No SQL Server table hints detected");
        }

        return TranspileRuleResult.unsupported(
            statement,
            "UNSUPPORTED_SQLSERVER_TABLE_HINTS",
            "SQL Server table hints cannot be transpiled exactly to non-SQL Server targets"
        );
    }
}
