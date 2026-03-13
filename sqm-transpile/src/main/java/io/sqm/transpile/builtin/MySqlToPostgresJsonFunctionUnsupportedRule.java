package io.sqm.transpile.builtin;

import io.sqm.core.Statement;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.transpile.TranspileContext;
import io.sqm.transpile.TranspileRuleResult;
import io.sqm.transpile.rule.TranspileRule;

import java.util.Set;

/**
 * Rejects representative MySQL JSON function families without a safe PostgreSQL rewrite in the current slice.
 */
public final class MySqlToPostgresJsonFunctionUnsupportedRule implements TranspileRule {
    /**
     * Creates a MySQL-to-PostgreSQL JSON-function rejection rule.
     */
    public MySqlToPostgresJsonFunctionUnsupportedRule() {
    }

    @Override
    public String id() {
        return "mysql-to-postgres-json-function-unsupported";
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
        if (!StatementFeatureInspector.hasAnyFunctionNamePrefix(statement, java.util.Set.of("JSON_"))) {
            return TranspileRuleResult.unchanged(statement, "No unsupported MySQL JSON function family detected");
        }
        return TranspileRuleResult.unsupported(
            statement,
            "UNSUPPORTED_MYSQL_JSON_FUNCTION",
            "MySQL JSON function families are not supported for PostgreSQL transpilation in the current slice"
        );
    }
}
