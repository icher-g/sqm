package io.sqm.validate.schema.rule;

import io.sqm.core.AnyAllPredicate;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import io.sqm.validate.schema.model.DbType;

/**
 * Validates type compatibility for ANY / ALL predicates.
 */
final class AnyAllPredicateTypeValidationRule implements SchemaValidationRule<AnyAllPredicate> {
    private final ProjectionShapeInspector projectionShapeInspector;

    /**
     * Creates ANY/ALL predicate validation rule.
     *
     * @param projectionShapeInspector projection shape inspector.
     */
    AnyAllPredicateTypeValidationRule(ProjectionShapeInspector projectionShapeInspector) {
        this.projectionShapeInspector = projectionShapeInspector;
    }

    /**
     * Returns supported node type.
     *
     * @return ANY/ALL predicate type.
     */
    @Override
    public Class<AnyAllPredicate> nodeType() {
        return AnyAllPredicate.class;
    }

    /**
     * Validates that left side and subquery projection are comparable.
     *
     * @param node ANY/ALL predicate node.
     * @param context schema validation context.
     */
    @Override
    public void validate(AnyAllPredicate node, SchemaValidationContext context) {
        if (!projectionShapeInspector.isSingleExpressionProjection(node.subquery())) {
            context.addProblem(
                ValidationProblem.Code.SUBQUERY_SHAPE_MISMATCH,
                "ANY/ALL subquery must project exactly one expression column",
                node,
                "predicate.any_all"
            );
            return;
        }
        var leftType = context.inferType(node.lhs());
        var rightType = context.inferSingleColumnType(node.subquery());
        if (leftType.isPresent() && rightType.isPresent() && !DbType.comparable(leftType.get(), rightType.get())) {
            context.addProblem(
                ValidationProblem.Code.TYPE_MISMATCH,
                "Incompatible ANY/ALL types: " + leftType.get() + " and " + rightType.get(),
                node,
                "predicate.any_all"
            );
        }
    }
}

