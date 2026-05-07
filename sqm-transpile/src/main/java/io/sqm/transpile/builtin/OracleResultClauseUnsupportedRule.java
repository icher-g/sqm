package io.sqm.transpile.builtin;

import io.sqm.core.Statement;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.transpile.TranspileContext;
import io.sqm.transpile.TranspileRuleResult;
import io.sqm.transpile.rule.TranspileRule;

import java.util.Set;

/**
 * Rejects generic DML result clauses when targeting Oracle.
 */
public final class OracleResultClauseUnsupportedRule implements TranspileRule {
    /**
     * Creates an Oracle result-clause rejection rule.
     */
    public OracleResultClauseUnsupportedRule() {
    }

    @Override
    public String id() {
        return "oracle-result-clause-unsupported";
    }

    @Override
    public Set<SqlDialectId> sourceDialects() {
        return Set.of();
    }

    @Override
    public Set<SqlDialectId> targetDialects() {
        return Set.of(SqlDialectId.ORACLE);
    }

    @Override
    public int order() {
        return 95;
    }

    @Override
    public TranspileRuleResult apply(Statement statement, TranspileContext context) {
        if (!StatementFeatureInspector.hasResultClause(statement)) {
            return TranspileRuleResult.unchanged(statement, "No DML result clause usage detected");
        }
        return TranspileRuleResult.unsupported(
            statement,
            "UNSUPPORTED_ORACLE_RESULT_CLAUSE",
            "Oracle DML RETURNING requires INTO targets and cannot be transpiled from generic result clauses"
        );
    }
}
