package io.sqm.validate.schema.rule;

import io.sqm.core.SelectQuery;
import io.sqm.core.TopSpec;
import io.sqm.validate.schema.internal.SchemaValidationContext;

/**
 * Validates {@link TopSpec} expression types and non-negative literal values.
 */
final class TopSpecValidationRule implements SchemaValidationRule<SelectQuery> {
    private final ScalarSubqueryShapeValidator scalarSubqueryShapeValidator;

    /**
     * Creates TOP-spec validation rule.
     *
     * @param scalarSubqueryShapeValidator scalar-subquery shape validator.
     */
    TopSpecValidationRule(ScalarSubqueryShapeValidator scalarSubqueryShapeValidator) {
        this.scalarSubqueryShapeValidator = scalarSubqueryShapeValidator;
    }

    /**
     * Returns supported node type.
     *
     * @return select query type.
     */
    @Override
    public Class<SelectQuery> nodeType() {
        return SelectQuery.class;
    }

    /**
     * Validates TOP expression for a select query.
     *
     * @param node    select query node.
     * @param context schema validation context.
     */
    @Override
    public void validate(SelectQuery node, SchemaValidationContext context) {
        var topSpec = node.topSpec();
        if (topSpec == null) {
            return;
        }
        LimitOffsetValidationRule.validateExpression(
            topSpec.count(),
            "top",
            node,
            context,
            scalarSubqueryShapeValidator
        );
    }
}
