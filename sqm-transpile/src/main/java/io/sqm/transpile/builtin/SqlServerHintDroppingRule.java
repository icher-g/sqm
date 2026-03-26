package io.sqm.transpile.builtin;

import io.sqm.core.Statement;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.transpile.RewriteFidelity;
import io.sqm.transpile.TranspileContext;
import io.sqm.transpile.TranspileRuleResult;
import io.sqm.transpile.rule.TranspileRule;

import java.util.Set;

/**
 * Drops SQL Server-native hints when transpiling to non-SQL Server dialects.
 */
public final class SqlServerHintDroppingRule implements TranspileRule {
    /**
     * Creates a SQL Server cross-dialect hint-dropping rule.
     */
    public SqlServerHintDroppingRule() {
    }

    @Override
    public String id() {
        return "sqlserver-hint-dropping";
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
        var transformer = new HintDroppingTransformer();
        var rewritten = transformer.transform(statement);
        if (rewritten == statement) {
            return TranspileRuleResult.unchanged(statement, "No SQL Server hints detected");
        }
        return TranspileRuleResult.rewrittenWithWarning(
            rewritten,
            RewriteFidelity.APPROXIMATE,
            "SQLSERVER_HINTS_DROPPED",
            "SQL Server hints were dropped during non-SQL Server transpilation",
            "Dropped SQL Server hints for non-SQL Server transpilation"
        );
    }
}
