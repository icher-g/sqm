package io.sqm.transpile.builtin;

import io.sqm.core.Statement;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.transpile.RewriteFidelity;
import io.sqm.transpile.TranspileContext;
import io.sqm.transpile.TranspileRuleResult;
import io.sqm.transpile.rule.TranspileRule;

import java.util.Set;

/**
 * Drops Oracle-native hints when transpiling to non-Oracle dialects.
 */
public final class OracleHintDroppingRule implements TranspileRule {
    /**
     * Creates an Oracle cross-dialect hint-dropping rule.
     */
    public OracleHintDroppingRule() {
    }

    @Override
    public String id() {
        return "oracle-hint-dropping";
    }

    @Override
    public Set<SqlDialectId> sourceDialects() {
        return Set.of(SqlDialectId.ORACLE);
    }

    @Override
    public Set<SqlDialectId> targetDialects() {
        return Set.of(SqlDialectId.ANSI, SqlDialectId.MYSQL, SqlDialectId.POSTGRESQL, SqlDialectId.SQLSERVER);
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
            return TranspileRuleResult.unchanged(statement, "No Oracle hints detected");
        }
        return TranspileRuleResult.rewrittenWithWarning(
            rewritten,
            RewriteFidelity.APPROXIMATE,
            "ORACLE_HINTS_DROPPED",
            "Oracle hints were dropped during non-Oracle transpilation",
            "Dropped Oracle hints for non-Oracle transpilation"
        );
    }
}
