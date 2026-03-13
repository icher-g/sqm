package io.sqm.transpile.builtin;

import io.sqm.core.Statement;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.transpile.TranspileContext;
import io.sqm.transpile.TranspileRuleResult;
import io.sqm.transpile.rule.TranspileRule;

import java.util.Set;

/**
 * Rejects representative PostgreSQL operator families without safe MySQL equivalents.
 */
public final class PostgresToMySqlOperatorFamilyUnsupportedRule implements TranspileRule {
    private static final Set<String> UNSUPPORTED_OPERATORS = Set.of(
        "->",
        "->>",
        "#>",
        "#>>",
        "@>",
        "<@",
        "?",
        "?|",
        "?&",
        "&&",
        "@?",
        "@@",
        "-|-",
        "&<",
        "&>"
    );

    /**
     * Creates a PostgreSQL-to-MySQL operator-family rejection rule.
     */
    public PostgresToMySqlOperatorFamilyUnsupportedRule() {
    }

    @Override
    public String id() {
        return "postgres-to-mysql-operator-family-unsupported";
    }

    @Override
    public Set<SqlDialectId> sourceDialects() {
        return Set.of(SqlDialectId.of("postgresql"));
    }

    @Override
    public Set<SqlDialectId> targetDialects() {
        return Set.of(SqlDialectId.of("mysql"));
    }

    @Override
    public int order() {
        return 100;
    }

    @Override
    public TranspileRuleResult apply(Statement statement, TranspileContext context) {
        if (!StatementFeatureInspector.hasAnyBinaryOperator(statement, UNSUPPORTED_OPERATORS)) {
            return TranspileRuleResult.unchanged(statement, "No unsupported PostgreSQL operator family detected");
        }
        return TranspileRuleResult.unsupported(
            statement,
            "UNSUPPORTED_POSTGRES_OPERATOR_FAMILY",
            "PostgreSQL-specific operator families are not supported for MySQL transpilation"
        );
    }
}
