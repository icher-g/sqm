package io.sqm.validate.schema.rule;

import io.sqm.core.ComparisonPredicate;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import io.sqm.validate.schema.model.CatalogTypeSemantics;

/**
 * Validates comparison operand type compatibility.
 */
final class ComparisonTypeValidationRule implements SchemaValidationRule<ComparisonPredicate> {
    private final ScalarSubqueryShapeValidator scalarSubqueryShapeValidator;

    /**
     * Creates comparison validation rule.
     *
     * @param scalarSubqueryShapeValidator scalar-subquery shape validator.
     */
    ComparisonTypeValidationRule(ScalarSubqueryShapeValidator scalarSubqueryShapeValidator) {
        this.scalarSubqueryShapeValidator = scalarSubqueryShapeValidator;
    }

    /**
     * Returns supported node type.
     *
     * @return comparison predicate type.
     */
    @Override
    public Class<ComparisonPredicate> nodeType() {
        return ComparisonPredicate.class;
    }

    /**
     * Validates that both comparison operands are type-compatible.
     *
     * @param node    comparison predicate node.
     * @param context schema validation context.
     */
    @Override
    public void validate(ComparisonPredicate node, SchemaValidationContext context) {
        var leftScalar = scalarSubqueryShapeValidator.ensureScalarSubquery(
            node.lhs(),
            node,
            "predicate.comparison",
            context,
            "Comparison left operand"
        );
        var rightScalar = scalarSubqueryShapeValidator.ensureScalarSubquery(
            node.rhs(),
            node,
            "predicate.comparison",
            context,
            "Comparison right operand"
        );
        if (!leftScalar || !rightScalar) {
            return;
        }
        var leftType = context.inferType(node.lhs());
        var rightType = context.inferType(node.rhs());
        if (leftType.isPresent() && rightType.isPresent() && !CatalogTypeSemantics.comparable(leftType.get(), rightType.get())) {
            context.addProblem(
                ValidationProblem.Code.TYPE_MISMATCH,
                "Incompatible comparison types: " + leftType.get() + " and " + rightType.get(),
                node,
                "predicate.comparison"
            );
        }
    }
}


