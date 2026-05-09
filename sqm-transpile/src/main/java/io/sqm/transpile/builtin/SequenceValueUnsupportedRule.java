package io.sqm.transpile.builtin;

import io.sqm.core.Statement;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.transpile.TranspileContext;
import io.sqm.transpile.TranspileRuleResult;
import io.sqm.transpile.rule.TranspileRule;

import java.util.Set;

/**
 * Rejects sequence value expressions when the target dialect cannot represent them exactly.
 */
public final class SequenceValueUnsupportedRule implements TranspileRule {
    /**
     * Creates a sequence value expression rejection rule.
     */
    public SequenceValueUnsupportedRule() {
    }

    @Override
    public String id() {
        return "sequence-value-unsupported";
    }

    @Override
    public Set<SqlDialectId> sourceDialects() {
        return Set.of();
    }

    @Override
    public Set<SqlDialectId> targetDialects() {
        return Set.of(SqlDialectId.ANSI, SqlDialectId.MYSQL, SqlDialectId.SQLSERVER);
    }

    @Override
    public TranspileRuleResult apply(Statement statement, TranspileContext context) {
        if (!StatementFeatureInspector.hasSequenceValueExpression(statement)) {
            return TranspileRuleResult.unchanged(statement, "No sequence value expression detected");
        }
        if (context.targetDialect() == SqlDialectId.SQLSERVER) {
            if (StatementFeatureInspector.hasCurrentSequenceValueExpression(statement)) {
                return TranspileRuleResult.unsupported(
                    statement,
                    "UNSUPPORTED_SEQUENCE_CURRENT_VALUE",
                    "SQL Server does not support current sequence value expressions"
                );
            }
            return TranspileRuleResult.unchanged(statement, "SQL Server supports next sequence value expressions");
        }
        return TranspileRuleResult.unsupported(
            statement,
            "UNSUPPORTED_SEQUENCE_VALUE",
            "Sequence value expressions are not supported for " + context.targetDialect().value() + " transpilation"
        );
    }
}
