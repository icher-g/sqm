package io.sqm.transpile.builtin;

import io.sqm.core.Statement;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.transpile.TranspileContext;
import io.sqm.transpile.TranspileRuleResult;
import io.sqm.transpile.rule.TranspileRule;

import java.util.Set;

/**
 * Rejects PostgreSQL DML {@code RETURNING} clauses when targeting MySQL.
 */
public final class PostgresToMySqlReturningUnsupportedRule implements TranspileRule {
    /**
     * Creates a PostgreSQL-to-MySQL returning rejection rule.
     */
    public PostgresToMySqlReturningUnsupportedRule() {
    }

    @Override
    public String id() {
        return "postgres-to-mysql-returning-unsupported";
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
        if (!StatementFeatureInspector.hasReturning(statement)) {
            return TranspileRuleResult.unchanged(statement, "No RETURNING usage detected");
        }
        return TranspileRuleResult.unsupported(
            statement,
            "UNSUPPORTED_RETURNING",
            "PostgreSQL RETURNING is not supported for MySQL transpilation"
        );
    }
}
