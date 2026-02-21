package io.sqm.validate.schema.rule;

import io.sqm.core.LikePredicate;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import io.sqm.catalog.model.CatalogType;
import io.sqm.validate.schema.model.CatalogTypeSemantics;

/**
 * Validates type compatibility for LIKE-family predicates.
 */
final class LikeTypeValidationRule implements SchemaValidationRule<LikePredicate> {
    private final ScalarSubqueryShapeValidator scalarSubqueryShapeValidator;

    /**
     * Creates LIKE validation rule.
     *
     * @param scalarSubqueryShapeValidator scalar-subquery shape validator.
     */
    LikeTypeValidationRule(ScalarSubqueryShapeValidator scalarSubqueryShapeValidator) {
        this.scalarSubqueryShapeValidator = scalarSubqueryShapeValidator;
    }

    /**
     * Returns supported node type.
     *
     * @return LIKE predicate type.
     */
    @Override
    public Class<LikePredicate> nodeType() {
        return LikePredicate.class;
    }

    /**
     * Validates that value, pattern, and escape expressions are string-compatible.
     *
     * @param node LIKE predicate node.
     * @param context schema validation context.
     */
    @Override
    public void validate(LikePredicate node, SchemaValidationContext context) {
        var valueScalar = scalarSubqueryShapeValidator.ensureScalarSubquery(
            node.value(),
            node,
            "predicate.like",
            context,
            "LIKE value"
        );
        var patternScalar = scalarSubqueryShapeValidator.ensureScalarSubquery(
            node.pattern(),
            node,
            "predicate.like",
            context,
            "LIKE pattern"
        );
        var escapeScalar = scalarSubqueryShapeValidator.ensureScalarSubquery(
            node.escape(),
            node,
            "predicate.like",
            context,
            "LIKE escape"
        );
        if (!valueScalar || !patternScalar || !escapeScalar) {
            return;
        }
        ensureStringType(node.value(), "LIKE value", node, context);
        ensureStringType(node.pattern(), "LIKE pattern", node, context);
        ensureStringType(node.escape(), "LIKE escape", node, context);
    }

    /**
     * Records type mismatch if expression type is known and not STRING.
     *
     * @param expression expression to validate.
     * @param label message label for diagnostics.
     * @param context schema validation context.
     */
    private static void ensureStringType(
        io.sqm.core.Expression expression,
        String label,
        LikePredicate node,
        SchemaValidationContext context
    ) {
        if (expression == null) {
            return;
        }
        var inferredType = context.inferType(expression);
        if (inferredType.isPresent()
            && CatalogTypeSemantics.isKnown(inferredType.get())
            && inferredType.get() != CatalogType.STRING) {
            context.addProblem(
                ValidationProblem.Code.TYPE_MISMATCH,
                label + " must be STRING but was " + inferredType.get(),
                node,
                "predicate.like"
            );
        }
    }
}



