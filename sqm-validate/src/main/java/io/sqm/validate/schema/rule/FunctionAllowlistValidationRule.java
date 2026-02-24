package io.sqm.validate.schema.rule;

import io.sqm.core.FunctionExpr;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;

/**
 * Validates function usage against schema access policy allowlist.
 */
final class FunctionAllowlistValidationRule implements SchemaValidationRule<FunctionExpr> {
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
     * Validates function allowlist policy.
     *
     * @param node function expression node.
     * @param context schema validation context.
     */
    @Override
    public void validate(FunctionExpr node, SchemaValidationContext context) {
        var functionName = functionName(node);
        if (context.isFunctionAllowed(functionName)) {
            return;
        }
        context.addProblem(
            ValidationProblem.Code.POLICY_FUNCTION_NOT_ALLOWED,
            "Function is not allowed by policy: " + functionName,
            node,
            "function.call"
        );
    }

    private static String functionName(FunctionExpr node) {
        return node.name() == null ? null : node.name().parts().getLast().value();
    }
}
