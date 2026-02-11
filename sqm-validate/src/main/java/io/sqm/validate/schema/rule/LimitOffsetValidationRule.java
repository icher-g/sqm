package io.sqm.validate.schema.rule;

import io.sqm.core.CompositeQuery;
import io.sqm.core.Expression;
import io.sqm.core.LimitOffset;
import io.sqm.core.LiteralExpr;
import io.sqm.core.SelectQuery;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import io.sqm.validate.schema.model.DbType;

/**
 * Validates LIMIT/OFFSET expression types and non-negative literal values.
 */
final class LimitOffsetValidationRule implements SchemaValidationRule<SelectQuery> {
    private final ScalarSubqueryShapeValidator scalarSubqueryShapeValidator;

    /**
     * Creates LIMIT/OFFSET validation rule.
     *
     * @param scalarSubqueryShapeValidator scalar-subquery shape validator.
     */
    LimitOffsetValidationRule(ScalarSubqueryShapeValidator scalarSubqueryShapeValidator) {
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
     * Validates LIMIT/OFFSET for a select query.
     *
     * @param node select query node.
     * @param context schema validation context.
     */
    @Override
    public void validate(SelectQuery node, SchemaValidationContext context) {
        validateLimitOffset(node.limitOffset(), node, context, scalarSubqueryShapeValidator);
    }

    /**
     * Validates one limit/offset pair.
     *
     * @param limitOffset limit/offset clause.
     * @param owner owner node for diagnostics.
     * @param context schema validation context.
     */
    static void validateLimitOffset(
        LimitOffset limitOffset,
        io.sqm.core.Node owner,
        SchemaValidationContext context,
        ScalarSubqueryShapeValidator scalarSubqueryShapeValidator
    ) {
        if (limitOffset == null) {
            return;
        }
        validateExpression(limitOffset.limit(), "limit", owner, context, scalarSubqueryShapeValidator);
        validateExpression(limitOffset.offset(), "offset", owner, context, scalarSubqueryShapeValidator);
    }

    /**
     * Validates one LIMIT/OFFSET expression.
     *
     * @param expression expression to validate.
     * @param label limit/offset label.
     * @param owner owner node for diagnostics.
     * @param context schema validation context.
     */
    private static void validateExpression(
        Expression expression,
        String label,
        io.sqm.core.Node owner,
        SchemaValidationContext context,
        ScalarSubqueryShapeValidator scalarSubqueryShapeValidator
    ) {
        if (expression == null) {
            return;
        }
        var scalar = scalarSubqueryShapeValidator.ensureScalarSubquery(
            expression,
            owner,
            "limit_offset",
            context,
            label.toUpperCase() + "/OFFSET expression"
        );
        if (!scalar) {
            return;
        }
        var type = context.inferType(expression);
        if (type.isPresent() && DbType.isKnown(type.get()) && !DbType.isNumeric(type.get())) {
            context.addProblem(
                ValidationProblem.Code.LIMIT_OFFSET_INVALID,
                label + " expression must be numeric but was " + type.get(),
                owner,
                "limit_offset"
            );
            return;
        }

        if (expression instanceof LiteralExpr literal && literal.value() instanceof Number number) {
            if (number.doubleValue() < 0d) {
                context.addProblem(
                    ValidationProblem.Code.LIMIT_OFFSET_INVALID,
                    label + " expression must be >= 0 but was " + number,
                    owner,
                    "limit_offset"
                );
            }
        }
    }
}

/**
 * Validates LIMIT/OFFSET expression types and non-negative literal values for composite queries.
 */
final class CompositeLimitOffsetValidationRule implements SchemaValidationRule<CompositeQuery> {
    private final ScalarSubqueryShapeValidator scalarSubqueryShapeValidator;

    /**
     * Creates composite LIMIT/OFFSET validation rule.
     *
     * @param scalarSubqueryShapeValidator scalar-subquery shape validator.
     */
    CompositeLimitOffsetValidationRule(ScalarSubqueryShapeValidator scalarSubqueryShapeValidator) {
        this.scalarSubqueryShapeValidator = scalarSubqueryShapeValidator;
    }

    /**
     * Returns supported node type.
     *
     * @return composite query type.
     */
    @Override
    public Class<CompositeQuery> nodeType() {
        return CompositeQuery.class;
    }

    /**
     * Validates LIMIT/OFFSET for a composite query.
     *
     * @param node composite query node.
     * @param context schema validation context.
     */
    @Override
    public void validate(CompositeQuery node, SchemaValidationContext context) {
        LimitOffsetValidationRule.validateLimitOffset(node.limitOffset(), node, context, scalarSubqueryShapeValidator);
    }
}
