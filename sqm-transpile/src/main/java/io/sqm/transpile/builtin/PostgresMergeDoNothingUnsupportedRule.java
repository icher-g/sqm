package io.sqm.transpile.builtin;

import io.sqm.core.Statement;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.transpile.TranspileContext;
import io.sqm.transpile.TranspileRuleResult;
import io.sqm.transpile.rule.TranspileRule;

import java.util.Set;

/**
 * Rejects PostgreSQL MERGE {@code DO NOTHING} actions for targets that do not support them.
 */
public final class PostgresMergeDoNothingUnsupportedRule implements TranspileRule {
    /**
     * Creates a PostgreSQL MERGE do-nothing rejection rule.
     */
    public PostgresMergeDoNothingUnsupportedRule() {
    }

    @Override
    public String id() {
        return "postgres-merge-do-nothing-unsupported";
    }

    @Override
    public Set<SqlDialectId> sourceDialects() {
        return Set.of(SqlDialectId.POSTGRESQL);
    }

    @Override
    public Set<SqlDialectId> targetDialects() {
        return Set.of(SqlDialectId.ORACLE, SqlDialectId.SQLSERVER);
    }

    @Override
    public int order() {
        return 90;
    }

    @Override
    public TranspileRuleResult apply(Statement statement, TranspileContext context) {
        if (!StatementFeatureInspector.hasMergeDoNothingAction(statement)) {
            return TranspileRuleResult.unchanged(statement, "No PostgreSQL MERGE DO NOTHING usage detected");
        }
        return TranspileRuleResult.unsupported(
            statement,
            "UNSUPPORTED_MERGE_DO_NOTHING",
            "PostgreSQL MERGE DO NOTHING actions cannot be transpiled exactly to this target dialect"
        );
    }
}
