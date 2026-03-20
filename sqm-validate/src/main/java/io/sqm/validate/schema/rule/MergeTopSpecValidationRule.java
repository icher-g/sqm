package io.sqm.validate.schema.rule;

import io.sqm.core.MergeStatement;
import io.sqm.core.LiteralExpr;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import io.sqm.validate.schema.model.CatalogTypeSemantics;

/**
 * Validates {@code MERGE TOP} expression types and non-negative literal values.
 */
final class MergeTopSpecValidationRule implements SchemaValidationRule<MergeStatement> {
    private final ScalarSubqueryShapeValidator scalarSubqueryShapeValidator;

    /**
     * Creates MERGE-top validation rule.
     *
     * @param scalarSubqueryShapeValidator scalar-subquery shape validator.
     */
    MergeTopSpecValidationRule(ScalarSubqueryShapeValidator scalarSubqueryShapeValidator) {
        this.scalarSubqueryShapeValidator = scalarSubqueryShapeValidator;
    }

    @Override
    public Class<MergeStatement> nodeType() {
        return MergeStatement.class;
    }

    /**
     * Validates MERGE TOP expression shape, type, and literal bounds.
     *
     * @param node merge statement node.
     * @param context schema validation context.
     */
    @Override
    public void validate(MergeStatement node, SchemaValidationContext context) {
        var topSpec = node.topSpec();
        if (topSpec == null) {
            return;
        }
        var expression = topSpec.count();
        var scalar = scalarSubqueryShapeValidator.ensureScalarSubquery(
            expression,
            node,
            "merge.top",
            context,
            "MERGE TOP expression"
        );
        if (!scalar) {
            return;
        }
        var type = context.inferType(expression);
        if (type.isPresent() && CatalogTypeSemantics.isKnown(type.get()) && !CatalogTypeSemantics.isNumeric(type.get())) {
            context.addProblem(
                ValidationProblem.Code.LIMIT_OFFSET_INVALID,
                "merge top expression must be numeric but was " + type.get(),
                node,
                "merge.top"
            );
            return;
        }

        if (expression instanceof LiteralExpr literal && literal.value() instanceof Number number && number.doubleValue() < 0d) {
            context.addProblem(
                ValidationProblem.Code.LIMIT_OFFSET_INVALID,
                "merge top expression must be >= 0 but was " + number,
                node,
                "merge.top"
            );
        }
    }
}
