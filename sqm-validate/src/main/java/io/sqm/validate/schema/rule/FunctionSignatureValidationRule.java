package io.sqm.validate.schema.rule;

import io.sqm.core.Expression;
import io.sqm.core.FunctionExpr;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.function.FunctionArgKind;
import io.sqm.validate.schema.function.FunctionCatalog;
import io.sqm.validate.schema.function.FunctionSignature;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import io.sqm.catalog.model.CatalogType;
import io.sqm.validate.schema.model.CatalogTypeSemantics;

import java.util.List;
import java.util.Optional;

/**
 * Validates function signatures by arity and basic argument types.
 */
final class FunctionSignatureValidationRule implements SchemaValidationRule<FunctionExpr> {
    private final FunctionCatalog functionCatalog;

    /**
     * Creates function signature rule.
     *
     * @param functionCatalog function catalog.
     */
    FunctionSignatureValidationRule(FunctionCatalog functionCatalog) {
        this.functionCatalog = functionCatalog;
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
     * Validates known function names; unknown names are ignored.
     *
     * @param node function expression node.
     * @param context schema validation context.
     */
    @Override
    public void validate(FunctionExpr node, SchemaValidationContext context) {
        var signature = functionCatalog.resolve(node.name());
        if (signature.isEmpty()) {
            return;
        }
        var args = node.args() == null ? List.<FunctionExpr.Arg>of() : node.args();
        if (!validateArity(node, args, signature.get(), context)) {
            return;
        }
        for (int i = 0; i < args.size(); i++) {
            var argKind = resolveArgKind(signature.get(), i);
            if (!validateArgKind(argKind, args.get(i), node, context)) {
                return;
            }
        }
    }

    /**
     * Resolves argument kind for argument index.
     *
     * @param signature function signature.
     * @param argumentIndex argument index.
     * @return expected argument kind.
     */
    private static FunctionArgKind resolveArgKind(FunctionSignature signature, int argumentIndex) {
        if (signature.argKinds().isEmpty()) {
            return FunctionArgKind.ANY_EXPR;
        }
        if (argumentIndex < signature.argKinds().size()) {
            return signature.argKinds().get(argumentIndex);
        }
        return signature.argKinds().getLast();
    }

    /**
     * Validates argument count range.
     *
     * @param node function node.
     * @param args function arguments.
     * @param signature function signature.
     * @param context validation context.
     * @return true when arity is valid.
     */
    private static boolean validateArity(
        FunctionExpr node,
        List<FunctionExpr.Arg> args,
        FunctionSignature signature,
        SchemaValidationContext context
    ) {
        if (args.size() < signature.minArity() || args.size() > signature.maxArity()) {
            var maxText = signature.maxArity() == Integer.MAX_VALUE ? "N" : String.valueOf(signature.maxArity());
            mismatch(node, "expects " + signature.minArity() + ".." + maxText + " argument(s)", context);
            return false;
        }
        return true;
    }

    /**
     * Validates one argument against expected kind.
     *
     * @param argKind expected argument kind.
     * @param arg actual argument.
     * @param node function node.
     * @param context validation context.
     * @return true when argument is valid.
     */
    private static boolean validateArgKind(
        FunctionArgKind argKind,
        FunctionExpr.Arg arg,
        FunctionExpr node,
        SchemaValidationContext context
    ) {
        return switch (argKind) {
            case STAR_OR_EXPR -> {
                if (arg instanceof FunctionExpr.Arg.StarArg || arg instanceof FunctionExpr.Arg.ExprArg) {
                    yield true;
                }
                mismatch(node, "expects expression or * argument", context);
                yield false;
            }
            case ANY_EXPR -> {
                if (arg instanceof FunctionExpr.Arg.ExprArg) {
                    yield true;
                }
                mismatch(node, "expects expression argument", context);
                yield false;
            }
            case STRING_EXPR -> validateTypedExprArg(arg, node, context, CatalogType.STRING, "expects STRING argument");
            case NUMERIC_EXPR -> validateNumericExprArg(arg, node, context);
        };
    }

    /**
     * Validates typed expression argument.
     *
     * @param arg function argument.
     * @param node function node.
     * @param context validation context.
     * @param expectedType expected type.
     * @param message mismatch message.
     * @return true when argument is valid.
     */
    private static boolean validateTypedExprArg(
        FunctionExpr.Arg arg,
        FunctionExpr node,
        SchemaValidationContext context,
        CatalogType expectedType,
        String message
    ) {
        var exprArg = asExprArg(arg, node, context);
        if (exprArg.isEmpty()) {
            return false;
        }
        var type = context.inferType(exprArg.get());
        if (type.isPresent() && CatalogTypeSemantics.isKnown(type.get()) && type.get() != expectedType) {
            mismatch(node, message, context);
            return false;
        }
        return true;
    }

    /**
     * Validates numeric expression argument.
     *
     * @param arg function argument.
     * @param node function node.
     * @param context validation context.
     * @return true when argument is valid.
     */
    private static boolean validateNumericExprArg(
        FunctionExpr.Arg arg,
        FunctionExpr node,
        SchemaValidationContext context
    ) {
        var exprArg = asExprArg(arg, node, context);
        if (exprArg.isEmpty()) {
            return false;
        }
        var type = context.inferType(exprArg.get());
        if (type.isPresent() && CatalogTypeSemantics.isKnown(type.get()) && !CatalogTypeSemantics.isNumeric(type.get())) {
            mismatch(node, "expects numeric argument", context);
            return false;
        }
        return true;
    }

    /**
     * Extracts expression argument or reports unsupported argument shape.
     *
     * @param arg function argument.
     * @param node function node.
     * @param context validation context.
     * @return expression argument.
     */
    private static Optional<Expression> asExprArg(
        FunctionExpr.Arg arg,
        FunctionExpr node,
        SchemaValidationContext context
    ) {
        if (arg instanceof FunctionExpr.Arg.ExprArg exprArg) {
            return Optional.of(exprArg.expr());
        }
        mismatch(node, "function expects expression argument", context);
        return Optional.empty();
    }

    /**
     * Records function signature mismatch.
     *
     * @param node function node.
     * @param details mismatch details.
     * @param context validation context.
     */
    private static void mismatch(FunctionExpr node, String details, SchemaValidationContext context) {
        context.addProblem(
            ValidationProblem.Code.FUNCTION_SIGNATURE_MISMATCH,
            "Function signature mismatch for '" + node.name() + "': " + details,
            node,
            "function.call"
        );
    }
}


