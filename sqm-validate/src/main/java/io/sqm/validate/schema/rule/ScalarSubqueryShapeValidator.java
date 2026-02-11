package io.sqm.validate.schema.rule;

import io.sqm.core.Expression;
import io.sqm.core.Node;
import io.sqm.validate.schema.internal.SchemaValidationContext;

/**
 * Validates subquery projection shape in scalar-expression contexts.
 */
interface ScalarSubqueryShapeValidator {
    /**
     * Validates that expression is scalar when it is a subquery expression.
     *
     * @param expression expression to check.
     * @param owner owner node for diagnostics.
     * @param clausePath clause path for diagnostics.
     * @param context validation context.
     * @param contextLabel short label describing scalar context.
     * @return true when shape is valid or expression is not a subquery expression.
     */
    boolean ensureScalarSubquery(
        Expression expression,
        Node owner,
        String clausePath,
        SchemaValidationContext context,
        String contextLabel
    );
}
