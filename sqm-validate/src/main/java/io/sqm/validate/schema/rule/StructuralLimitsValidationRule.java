package io.sqm.validate.schema.rule;

import io.sqm.core.SelectQuery;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.SchemaValidationLimits;
import io.sqm.validate.schema.internal.SchemaValidationContext;

/**
 * Validates structural limits for SELECT queries.
 */
final class StructuralLimitsValidationRule implements SchemaValidationRule<SelectQuery> {
    private final SchemaValidationLimits limits;

    /**
     * Creates structural limits validation rule.
     *
     * @param limits structural validation limits.
     */
    StructuralLimitsValidationRule(SchemaValidationLimits limits) {
        this.limits = limits;
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
     * Validates join and projection limits for a SELECT query.
     *
     * @param node select query node.
     * @param context schema validation context.
     */
    @Override
    public void validate(SelectQuery node, SchemaValidationContext context) {
        var maxJoinCount = limits.maxJoinCount();
        if (maxJoinCount != null && node.joins().size() > maxJoinCount) {
            context.addProblem(
                ValidationProblem.Code.POLICY_MAX_JOINS_EXCEEDED,
                "Join count exceeds configured maximum: " + node.joins().size() + " > " + maxJoinCount,
                node,
                "from.join"
            );
        }

        var maxSelectColumns = limits.maxSelectColumns();
        if (maxSelectColumns != null && node.items().size() > maxSelectColumns) {
            context.addProblem(
                ValidationProblem.Code.POLICY_MAX_SELECT_COLUMNS_EXCEEDED,
                "Projected column count exceeds configured maximum: " + node.items().size() + " > " + maxSelectColumns,
                node,
                "select"
            );
        }
    }
}
