package io.sqm.transpile.builtin;

import io.sqm.core.InsertStatement;
import io.sqm.core.Statement;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.transpile.TranspileContext;
import io.sqm.transpile.TranspileRuleResult;
import io.sqm.transpile.rule.TranspileRule;

import java.util.Set;

/**
 * Rejects MySQL-only insert modes without an exact PostgreSQL rewrite in the current slice.
 */
public final class MySqlToPostgresInsertModeUnsupportedRule implements TranspileRule {
    /**
     * Creates a MySQL-to-PostgreSQL insert-mode rejection rule.
     */
    public MySqlToPostgresInsertModeUnsupportedRule() {
    }

    @Override
    public String id() {
        return "mysql-to-postgres-insert-mode-unsupported";
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
        if (StatementFeatureInspector.hasInsertMode(statement, InsertStatement.InsertMode.IGNORE)) {
            return TranspileRuleResult.unsupported(
                statement,
                "UNSUPPORTED_INSERT_IGNORE",
                "MySQL INSERT IGNORE is not supported for PostgreSQL transpilation"
            );
        }
        if (StatementFeatureInspector.hasInsertMode(statement, InsertStatement.InsertMode.REPLACE)) {
            return TranspileRuleResult.unsupported(
                statement,
                "UNSUPPORTED_REPLACE_INTO",
                "MySQL REPLACE INTO is not supported for PostgreSQL transpilation"
            );
        }
        return TranspileRuleResult.unchanged(statement, "No unsupported MySQL insert mode detected");
    }
}
