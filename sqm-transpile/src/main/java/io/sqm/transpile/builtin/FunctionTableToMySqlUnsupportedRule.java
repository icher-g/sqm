package io.sqm.transpile.builtin;

import io.sqm.core.Statement;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.transpile.TranspileContext;
import io.sqm.transpile.TranspileRuleResult;
import io.sqm.transpile.rule.TranspileRule;

import java.util.Set;

/**
 * Rejects generic function-table usage when targeting MySQL.
 *
 * <p>Current shipped MySQL support is modeled separately through
 * {@code JSON_TABLE(...)}-style semantics rather than the shared generic
 * {@code FunctionTable} node.</p>
 */
public final class FunctionTableToMySqlUnsupportedRule implements TranspileRule {
    /**
     * Creates a function-table-to-MySQL rejection rule.
     */
    public FunctionTableToMySqlUnsupportedRule() {
    }

    @Override
    public String id() {
        return "function-table-to-mysql-unsupported";
    }

    @Override
    public Set<SqlDialectId> sourceDialects() {
        return Set.of(SqlDialectId.POSTGRESQL, SqlDialectId.SQLSERVER);
    }

    @Override
    public Set<SqlDialectId> targetDialects() {
        return Set.of(SqlDialectId.MYSQL);
    }

    @Override
    public TranspileRuleResult apply(Statement statement, TranspileContext context) {
        if (!StatementFeatureInspector.hasFunctionTable(statement)) {
            return TranspileRuleResult.unchanged(statement, "No function-table usage detected");
        }
        return TranspileRuleResult.unsupported(
            statement,
            "UNSUPPORTED_FUNCTION_TABLE",
            "Generic function tables are not supported for MySQL transpilation"
        );
    }
}
