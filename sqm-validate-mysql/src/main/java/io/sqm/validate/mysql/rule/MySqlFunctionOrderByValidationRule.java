package io.sqm.validate.mysql.rule;

import io.sqm.core.FunctionExpr;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import io.sqm.validate.schema.rule.SchemaValidationRule;

/**
 * Rejects PostgreSQL-style aggregate input ordering for MySQL validation.
 */
public final class MySqlFunctionOrderByValidationRule implements SchemaValidationRule<FunctionExpr> {
    /**
     * Creates a MySQL function input ordering validation rule.
     */
    public MySqlFunctionOrderByValidationRule() {
    }

    /**
     * Returns supported node type.
     *
     * @return function expression type.
     */
    @Override
    public Class<FunctionExpr> nodeType() {
        return FunctionExpr.class;
    }

    /**
     * Validates MySQL function input ordering support.
     *
     * @param node function expression node.
     * @param context schema validation context.
     */
    @Override
    public void validate(FunctionExpr node, SchemaValidationContext context) {
        if (node.orderBy() == null) {
            return;
        }
        context.addProblem(
            ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED,
            "Function argument ORDER BY is not supported by the MySQL validation dialect",
            node,
            "function.orderBy"
        );
    }
}
