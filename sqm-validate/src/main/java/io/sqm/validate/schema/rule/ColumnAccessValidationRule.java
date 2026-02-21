package io.sqm.validate.schema.rule;

import io.sqm.core.ColumnExpr;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;

/**
 * Validates column access against schema access policy denylist.
 */
final class ColumnAccessValidationRule implements SchemaValidationRule<ColumnExpr> {
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
     * Validates column access policy.
     *
     * @param node column node.
     * @param context schema validation context.
     */
    @Override
    public void validate(ColumnExpr node, SchemaValidationContext context) {
        if (!context.accessPolicy().isColumnDenied(node.tableAlias(), node.name())) {
            return;
        }
        context.addProblem(
            ValidationProblem.Code.POLICY_COLUMN_DENIED,
            "Column is denied by policy: "
                + (node.tableAlias() == null ? node.name() : node.tableAlias() + "." + node.name()),
            node,
            "column.reference"
        );
    }
}
