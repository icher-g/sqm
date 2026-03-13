package io.sqm.transpile.builtin;

import io.sqm.core.LikeMode;
import io.sqm.core.Statement;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.transpile.TranspileContext;
import io.sqm.transpile.TranspileRuleResult;
import io.sqm.transpile.rule.TranspileRule;

import java.util.Set;

/**
 * Rejects PostgreSQL {@code SIMILAR TO} predicates when targeting MySQL.
 */
public final class PostgresToMySqlSimilarToUnsupportedRule implements TranspileRule {
    /**
     * Creates a PostgreSQL-to-MySQL similar-to rejection rule.
     */
    public PostgresToMySqlSimilarToUnsupportedRule() {
    }

    @Override
    public String id() {
        return "postgres-to-mysql-similar-to-unsupported";
    }

    @Override
    public Set<SqlDialectId> sourceDialects() {
        return Set.of(SqlDialectId.POSTGRESQL);
    }

    @Override
    public Set<SqlDialectId> targetDialects() {
        return Set.of(SqlDialectId.MYSQL);
    }

    @Override
    public int order() {
        return 100;
    }

    @Override
    public TranspileRuleResult apply(Statement statement, TranspileContext context) {
        if (!StatementFeatureInspector.hasLikeMode(statement, LikeMode.SIMILAR_TO)) {
            return TranspileRuleResult.unchanged(statement, "No SIMILAR TO usage detected");
        }
        return TranspileRuleResult.unsupported(
            statement,
            "UNSUPPORTED_SIMILAR_TO",
            "PostgreSQL SIMILAR TO is not supported for MySQL transpilation"
        );
    }
}
