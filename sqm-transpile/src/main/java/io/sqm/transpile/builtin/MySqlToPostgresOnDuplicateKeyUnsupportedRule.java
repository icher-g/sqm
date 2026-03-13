package io.sqm.transpile.builtin;

import io.sqm.core.InsertStatement;
import io.sqm.core.Statement;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.transpile.TranspileContext;
import io.sqm.transpile.TranspileRuleResult;
import io.sqm.transpile.rule.TranspileRule;

import java.util.Set;

/**
 * Rejects MySQL {@code ON DUPLICATE KEY UPDATE} for PostgreSQL transpilation in the current slice.
 */
public final class MySqlToPostgresOnDuplicateKeyUnsupportedRule implements TranspileRule {
    /**
     * Creates a MySQL-to-PostgreSQL duplicate-key rejection rule.
     */
    public MySqlToPostgresOnDuplicateKeyUnsupportedRule() {
    }

    @Override
    public String id() {
        return "mysql-to-postgres-on-duplicate-key-unsupported";
    }

    @Override
    public Set<SqlDialectId> sourceDialects() {
        return Set.of(SqlDialectId.MYSQL);
    }

    @Override
    public Set<SqlDialectId> targetDialects() {
        return Set.of(SqlDialectId.POSTGRESQL);
    }

    @Override
    public int order() {
        return 100;
    }

    @Override
    public TranspileRuleResult apply(Statement statement, TranspileContext context) {
        if (!StatementFeatureInspector.hasOnConflictAction(statement, InsertStatement.OnConflictAction.DO_UPDATE)) {
            return TranspileRuleResult.unchanged(statement, "No ON DUPLICATE KEY UPDATE usage detected");
        }
        return TranspileRuleResult.unsupported(
            statement,
            "UNSUPPORTED_ON_DUPLICATE_KEY_UPDATE",
            "MySQL ON DUPLICATE KEY UPDATE is not supported for PostgreSQL transpilation in the current slice"
        );
    }
}
