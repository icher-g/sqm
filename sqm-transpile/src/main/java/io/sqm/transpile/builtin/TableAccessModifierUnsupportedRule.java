package io.sqm.transpile.builtin;

import io.sqm.core.Statement;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.transpile.TranspileContext;
import io.sqm.transpile.TranspileRuleResult;
import io.sqm.transpile.rule.TranspileRule;

import java.util.Set;

/**
 * Conservatively rejects cross-dialect transpilation of table access modifiers.
 */
public final class TableAccessModifierUnsupportedRule implements TranspileRule {
    /**
     * Creates a table access modifier rejection rule.
     */
    public TableAccessModifierUnsupportedRule() {
    }

    @Override
    public String id() {
        return "table-access-modifier-unsupported";
    }

    @Override
    public Set<SqlDialectId> sourceDialects() {
        return Set.of();
    }

    @Override
    public Set<SqlDialectId> targetDialects() {
        return Set.of();
    }

    @Override
    public TranspileRuleResult apply(Statement statement, TranspileContext context) {
        if (!StatementFeatureInspector.hasTableAccessModifier(statement)) {
            return TranspileRuleResult.unchanged(statement, "No table access modifiers detected");
        }
        if (context.sourceDialect() == context.targetDialect()) {
            return TranspileRuleResult.unchanged(statement, "Source and target dialects match");
        }
        return TranspileRuleResult.unsupported(
            statement,
            "UNSUPPORTED_TABLE_ACCESS_MODIFIER",
            "Table access modifiers require explicit exact transpilation between "
                + context.sourceDialect().value() + " and " + context.targetDialect().value()
        );
    }
}
