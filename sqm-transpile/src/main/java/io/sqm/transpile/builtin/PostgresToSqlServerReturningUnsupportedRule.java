package io.sqm.transpile.builtin;

import io.sqm.core.Statement;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.transpile.TranspileContext;
import io.sqm.transpile.TranspileRuleResult;
import io.sqm.transpile.rule.TranspileRule;

import java.util.Set;

/**
 * Rejects PostgreSQL DML {@code RETURNING} clauses when targeting SQL Server.
 */
public final class PostgresToSqlServerReturningUnsupportedRule implements TranspileRule {
    /**
     * Creates a PostgreSQL-to-SQL Server returning rejection rule.
     */
    public PostgresToSqlServerReturningUnsupportedRule() {
    }

    @Override
    public String id() {
        return "postgres-to-sqlserver-returning-unsupported";
    }

    @Override
    public Set<SqlDialectId> sourceDialects() {
        return Set.of(SqlDialectId.POSTGRESQL);
    }

    @Override
    public Set<SqlDialectId> targetDialects() {
        return Set.of(SqlDialectId.SQLSERVER);
    }

    @Override
    public TranspileRuleResult apply(Statement statement, TranspileContext context) {
        if (!StatementFeatureInspector.hasResultClause(statement)) {
            return TranspileRuleResult.unchanged(statement, "No RETURNING usage detected");
        }
        return TranspileRuleResult.unsupported(
            statement,
            "UNSUPPORTED_RETURNING",
            "PostgreSQL RETURNING is not supported for SQL Server transpilation"
        );
    }
}
