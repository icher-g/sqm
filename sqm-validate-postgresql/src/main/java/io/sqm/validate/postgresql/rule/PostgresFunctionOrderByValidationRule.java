package io.sqm.validate.postgresql.rule;

import io.sqm.core.FunctionExpr;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.function.FunctionCatalog;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import io.sqm.validate.schema.rule.SchemaValidationRule;

import java.util.Locale;
import java.util.Objects;

/**
 * Validates PostgreSQL aggregate input ordering on function calls.
 */
public final class PostgresFunctionOrderByValidationRule implements SchemaValidationRule<FunctionExpr> {
    private final FunctionCatalog functionCatalog;

    /**
     * Creates a PostgreSQL function input ordering validation rule.
     *
     * @param functionCatalog function catalog used to identify known aggregate functions.
     */
    public PostgresFunctionOrderByValidationRule(FunctionCatalog functionCatalog) {
        this.functionCatalog = Objects.requireNonNull(functionCatalog, "functionCatalog");
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
     * Validates PostgreSQL function input ordering support.
     *
     * @param node function expression node.
     * @param context schema validation context.
     */
    @Override
    public void validate(FunctionExpr node, SchemaValidationContext context) {
        if (node.orderBy() == null) {
            return;
        }
        var signature = functionCatalog.resolve(functionName(node));
        if (signature.isEmpty() || signature.get().aggregate()) {
            return;
        }
        context.addProblem(
            ValidationProblem.Code.DIALECT_CLAUSE_INVALID,
            "Function argument ORDER BY is only valid for aggregate functions in PostgreSQL: " + functionName(node),
            node,
            "function.orderBy"
        );
    }

    private static String functionName(FunctionExpr node) {
        return node.name() == null ? null : node.name().parts().getLast().value().toLowerCase(Locale.ROOT);
    }
}
