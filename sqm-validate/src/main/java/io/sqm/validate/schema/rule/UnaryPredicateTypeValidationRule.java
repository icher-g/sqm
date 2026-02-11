package io.sqm.validate.schema.rule;

import io.sqm.core.UnaryPredicate;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import io.sqm.validate.schema.model.DbType;

/**
 * Validates that unary predicates use boolean-valued expressions.
 */
final class UnaryPredicateTypeValidationRule implements SchemaValidationRule<UnaryPredicate> {
    private final ScalarSubqueryShapeValidator scalarSubqueryShapeValidator;

    /**
     * Creates unary predicate validation rule.
     *
     * @param scalarSubqueryShapeValidator scalar-subquery shape validator.
     */
    UnaryPredicateTypeValidationRule(ScalarSubqueryShapeValidator scalarSubqueryShapeValidator) {
        this.scalarSubqueryShapeValidator = scalarSubqueryShapeValidator;
    }

    /**
     * Returns supported node type.
     *
     * @return unary predicate type.
     */
    @Override
    public Class<UnaryPredicate> nodeType() {
        return UnaryPredicate.class;
    }

    /**
     * Validates unary predicate expression type when inferable.
     *
     * @param node unary predicate node.
     * @param context schema validation context.
     */
    @Override
    public void validate(UnaryPredicate node, SchemaValidationContext context) {
        var scalar = scalarSubqueryShapeValidator.ensureScalarSubquery(
            node.expr(),
            node,
            "predicate.unary",
            context,
            "Unary predicate"
        );
        if (!scalar) {
            return;
        }
        var inferred = context.inferType(node.expr());
        if (inferred.isPresent() && DbType.isKnown(inferred.get()) && inferred.get() != DbType.BOOLEAN) {
            context.addProblem(
                ValidationProblem.Code.TYPE_MISMATCH,
                "Unary predicate expression must be BOOLEAN but was " + inferred.get(),
                node,
                "predicate.unary"
            );
        }
    }
}
