package io.sqm.transpile.builtin;

import io.sqm.core.Statement;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.transpile.RewriteFidelity;
import io.sqm.transpile.TranspileContext;
import io.sqm.transpile.TranspileRuleResult;
import io.sqm.transpile.rule.TranspileRule;

import java.util.Set;

/**
 * Drops MySQL-native hints when transpiling to non-MySQL dialects.
 */
public final class MySqlHintDroppingRule implements TranspileRule {
    /**
     * Creates a MySQL cross-dialect hint-dropping rule.
     */
    public MySqlHintDroppingRule() {
    }

    @Override
    public String id() {
        return "mysql-hint-dropping";
    }

    @Override
    public Set<SqlDialectId> sourceDialects() {
        return Set.of(SqlDialectId.MYSQL);
    }

    @Override
    public Set<SqlDialectId> targetDialects() {
        return Set.of(SqlDialectId.ANSI, SqlDialectId.POSTGRESQL, SqlDialectId.SQLSERVER);
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
            return TranspileRuleResult.unchanged(statement, "No MySQL hints detected");
        }
        return TranspileRuleResult.rewrittenWithWarning(
            rewritten,
            RewriteFidelity.APPROXIMATE,
            "MYSQL_HINTS_DROPPED",
            "MySQL hints were dropped during non-MySQL transpilation",
            "Dropped MySQL hints for non-MySQL transpilation"
        );
    }
}
