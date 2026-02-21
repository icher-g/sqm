package io.sqm.validate.schema.rule;

import io.sqm.core.BetweenPredicate;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import io.sqm.validate.schema.model.CatalogTypeSemantics;

/**
 * Validates type compatibility in BETWEEN predicates.
 */
final class BetweenTypeValidationRule implements SchemaValidationRule<BetweenPredicate> {
    private final ScalarSubqueryShapeValidator scalarSubqueryShapeValidator;

    /**
     * Creates BETWEEN validation rule.
     *
     * @param scalarSubqueryShapeValidator scalar-subquery shape validator.
     */
    BetweenTypeValidationRule(ScalarSubqueryShapeValidator scalarSubqueryShapeValidator) {
        this.scalarSubqueryShapeValidator = scalarSubqueryShapeValidator;
    }

    /**
     * Returns supported node type.
     *
     * @return BETWEEN predicate type.
     */
    @Override
    public Class<BetweenPredicate> nodeType() {
        return BetweenPredicate.class;
    }

    /**
     * Validates that value, lower, and upper operands are mutually comparable.
     *
     * @param node    BETWEEN predicate node.
     * @param context schema validation context.
     */
    @Override
    public void validate(BetweenPredicate node, SchemaValidationContext context) {
        var valueScalar = scalarSubqueryShapeValidator.ensureScalarSubquery(
            node.value(),
            node,
            "predicate.between",
            context,
            "BETWEEN value"
        );
        var lowerScalar = scalarSubqueryShapeValidator.ensureScalarSubquery(
            node.lower(),
            node,
            "predicate.between",
            context,
            "BETWEEN lower bound"
        );
        var upperScalar = scalarSubqueryShapeValidator.ensureScalarSubquery(
            node.upper(),
            node,
            "predicate.between",
            context,
            "BETWEEN upper bound"
        );
        if (!valueScalar || !lowerScalar || !upperScalar) {
            return;
        }
        var valueType = context.inferType(node.value());
        var lowerType = context.inferType(node.lower());
        var upperType = context.inferType(node.upper());

        if (valueType.isPresent() && lowerType.isPresent() && !CatalogTypeSemantics.comparable(valueType.get(), lowerType.get())) {
            context.addProblem(
                ValidationProblem.Code.TYPE_MISMATCH,
                "Incompatible BETWEEN types: " + valueType.get() + " and " + lowerType.get(),
                node,
                "predicate.between"
            );
        }
        if (valueType.isPresent() && upperType.isPresent() && !CatalogTypeSemantics.comparable(valueType.get(), upperType.get())) {
            context.addProblem(
                ValidationProblem.Code.TYPE_MISMATCH,
                "Incompatible BETWEEN types: " + valueType.get() + " and " + upperType.get(),
                node,
                "predicate.between"
            );
        }
    }
}



