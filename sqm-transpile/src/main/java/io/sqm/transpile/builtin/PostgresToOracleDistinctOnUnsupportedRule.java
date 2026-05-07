package io.sqm.transpile.builtin;

import io.sqm.core.Statement;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.transpile.TranspileContext;
import io.sqm.transpile.TranspileRuleResult;
import io.sqm.transpile.rule.TranspileRule;

import java.util.Set;

/**
 * Rejects PostgreSQL {@code DISTINCT ON} when targeting Oracle.
 */
public final class PostgresToOracleDistinctOnUnsupportedRule implements TranspileRule {
    /**
     * Creates a PostgreSQL-to-Oracle distinct-on rejection rule.
     */
    public PostgresToOracleDistinctOnUnsupportedRule() {
    }

    @Override
    public String id() {
        return "postgres-to-oracle-distinct-on-unsupported";
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
    public TranspileRuleResult apply(Statement statement, TranspileContext context) {
        if (!StatementFeatureInspector.hasDistinctOn(statement)) {
            return TranspileRuleResult.unchanged(statement, "No DISTINCT ON usage detected");
        }
        return TranspileRuleResult.unsupported(
            statement,
            "UNSUPPORTED_DISTINCT_ON",
            "PostgreSQL DISTINCT ON is not supported for Oracle transpilation"
        );
    }
}
