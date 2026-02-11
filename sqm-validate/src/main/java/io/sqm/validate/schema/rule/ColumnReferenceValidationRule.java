package io.sqm.validate.schema.rule;

import io.sqm.core.ColumnExpr;
import io.sqm.validate.schema.internal.SchemaValidationContext;

/**
 * Validates that referenced columns are resolvable in current scope.
 */
final class ColumnReferenceValidationRule implements SchemaValidationRule<ColumnExpr> {
    /**
     * Returns supported node type.
     *
     * @return column expression type.
     */
    @Override
    public Class<ColumnExpr> nodeType() {
        return ColumnExpr.class;
    }

    /**
     * Validates column reference resolution.
     *
     * @param node column node.
     * @param context schema validation context.
     */
    @Override
    public void validate(ColumnExpr node, SchemaValidationContext context) {
        context.resolveColumn(node, true);
    }
}
