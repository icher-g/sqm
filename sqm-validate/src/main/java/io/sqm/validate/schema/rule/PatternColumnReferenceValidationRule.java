package io.sqm.validate.schema.rule;

import io.sqm.core.PatternColumnExpr;
import io.sqm.validate.schema.internal.SchemaValidationContext;

/**
 * Resolves row-pattern column names against the input relation.
 */
final class PatternColumnReferenceValidationRule implements SchemaValidationRule<PatternColumnExpr> {
    @Override
    public Class<PatternColumnExpr> nodeType() {
        return PatternColumnExpr.class;
    }

    @Override
    public void validate(PatternColumnExpr node, SchemaValidationContext context) {
        if (context.inPatternExpressionScope()) {
            context.resolvePatternColumn(node.column(), node, context.patternExpressionPath());
        }
    }
}
