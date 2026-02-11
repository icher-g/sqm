package io.sqm.validate.schema.rule;

import io.sqm.core.IsDistinctFromPredicate;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import io.sqm.validate.schema.model.DbType;

/**
 * Validates type compatibility for IS DISTINCT FROM predicates.
 */
final class IsDistinctFromTypeValidationRule implements SchemaValidationRule<IsDistinctFromPredicate> {
    private final ScalarSubqueryShapeValidator scalarSubqueryShapeValidator;

    /**
     * Creates IS DISTINCT FROM validation rule.
     *
     * @param scalarSubqueryShapeValidator scalar-subquery shape validator.
     */
    IsDistinctFromTypeValidationRule(ScalarSubqueryShapeValidator scalarSubqueryShapeValidator) {
        this.scalarSubqueryShapeValidator = scalarSubqueryShapeValidator;
    }

    /**
     * Returns supported node type.
     *
     * @return IS DISTINCT FROM predicate type.
     */
    @Override
    public Class<IsDistinctFromPredicate> nodeType() {
        return IsDistinctFromPredicate.class;
    }

    /**
     * Validates that both operands are comparable when type information is known.
     *
     * @param node predicate node.
     * @param context schema validation context.
     */
    @Override
    public void validate(IsDistinctFromPredicate node, SchemaValidationContext context) {
        var leftScalar = scalarSubqueryShapeValidator.ensureScalarSubquery(
            node.lhs(),
            node,
            "predicate.is_distinct_from",
            context,
            "IS DISTINCT FROM left operand"
        );
        var rightScalar = scalarSubqueryShapeValidator.ensureScalarSubquery(
            node.rhs(),
            node,
            "predicate.is_distinct_from",
            context,
            "IS DISTINCT FROM right operand"
        );
        if (!leftScalar || !rightScalar) {
            return;
        }
        var leftType = context.inferType(node.lhs());
        var rightType = context.inferType(node.rhs());
        if (leftType.isPresent() && rightType.isPresent() && !DbType.comparable(leftType.get(), rightType.get())) {
            context.addProblem(
                ValidationProblem.Code.TYPE_MISMATCH,
                "Incompatible IS DISTINCT FROM types: " + leftType.get() + " and " + rightType.get(),
                node,
                "predicate.is_distinct_from"
            );
        }
    }
}

