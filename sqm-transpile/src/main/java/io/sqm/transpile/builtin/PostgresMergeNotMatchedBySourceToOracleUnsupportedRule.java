package io.sqm.transpile.builtin;

import io.sqm.core.Statement;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.transpile.TranspileContext;
import io.sqm.transpile.TranspileRuleResult;
import io.sqm.transpile.rule.TranspileRule;

import java.util.Set;

/**
 * Rejects PostgreSQL MERGE {@code WHEN NOT MATCHED BY SOURCE} clauses when targeting Oracle.
 */
public final class PostgresMergeNotMatchedBySourceToOracleUnsupportedRule implements TranspileRule {
    /**
     * Creates a PostgreSQL-to-Oracle MERGE not-matched-by-source rejection rule.
     */
    public PostgresMergeNotMatchedBySourceToOracleUnsupportedRule() {
    }

    @Override
    public String id() {
        return "postgres-merge-not-matched-by-source-to-oracle-unsupported";
    }

    @Override
    public Set<SqlDialectId> sourceDialects() {
        return Set.of(SqlDialectId.POSTGRESQL);
    }

    @Override
    public Set<SqlDialectId> targetDialects() {
        return Set.of(SqlDialectId.ORACLE);
    }

    @Override
    public int order() {
        return 90;
    }

    @Override
    public TranspileRuleResult apply(Statement statement, TranspileContext context) {
        if (!StatementFeatureInspector.hasMergeNotMatchedBySourceClause(statement)) {
            return TranspileRuleResult.unchanged(statement, "No PostgreSQL MERGE NOT MATCHED BY SOURCE usage detected");
        }
        return TranspileRuleResult.unsupported(
            statement,
            "UNSUPPORTED_MERGE_NOT_MATCHED_BY_SOURCE",
            "PostgreSQL MERGE WHEN NOT MATCHED BY SOURCE cannot be transpiled exactly to Oracle"
        );
    }
}
