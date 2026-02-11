package io.sqm.validate.schema.rule;

import io.sqm.core.GroupBy;
import io.sqm.core.GroupItem;
import io.sqm.core.ExprSelectItem;
import io.sqm.core.SelectQuery;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;

/**
 * Validates GROUP BY ordinals against the SELECT projection width.
 */
final class GroupByOrdinalValidationRule implements SchemaValidationRule<SelectQuery> {
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
     * Validates all GROUP BY ordinals in the provided SELECT query.
     *
     * @param query select query that may contain GROUP BY ordinals.
     * @param context schema validation context.
     */
    @Override
    public void validate(SelectQuery query, SchemaValidationContext context) {
        if (query.groupBy() == null) {
            return;
        }
        var maxOrdinal = query.items().size();
        validateItems(query, query.groupBy(), maxOrdinal, context);
    }

    /**
     * Validates GROUP BY items recursively to support grouping extensions.
     *
     * @param groupBy group-by clause.
     * @param maxOrdinal maximum allowed 1-based ordinal.
     * @param context schema validation context.
     */
    private static void validateItems(
        SelectQuery query,
        GroupBy groupBy,
        int maxOrdinal,
        SchemaValidationContext context
    ) {
        for (var item : groupBy.items()) {
            validateItem(query, item, maxOrdinal, context);
        }
    }

    /**
     * Validates one grouping item and traverses nested grouping structures.
     *
     * @param item grouping item.
     * @param maxOrdinal maximum allowed 1-based ordinal.
     * @param context schema validation context.
     */
    private static void validateItem(
        SelectQuery query,
        GroupItem item,
        int maxOrdinal,
        SchemaValidationContext context
    ) {
        switch (item) {
            case GroupItem.SimpleGroupItem simple -> validateSimpleItem(query, simple, maxOrdinal, context);
            case GroupItem.GroupingSet groupingSet -> groupingSet.items().forEach(i -> validateItem(query, i, maxOrdinal, context));
            case GroupItem.GroupingSets groupingSets -> groupingSets.sets().forEach(i -> validateItem(query, i, maxOrdinal, context));
            case GroupItem.Rollup rollup -> rollup.items().forEach(i -> validateItem(query, i, maxOrdinal, context));
            case GroupItem.Cube cube -> cube.items().forEach(i -> validateItem(query, i, maxOrdinal, context));
            default -> {
            }
        }
    }

    /**
     * Validates simple ordinal-based grouping item.
     *
     * @param item simple grouping item.
     * @param maxOrdinal maximum allowed 1-based ordinal.
     * @param context schema validation context.
     */
    private static void validateSimpleItem(
        SelectQuery query,
        GroupItem.SimpleGroupItem item,
        int maxOrdinal,
        SchemaValidationContext context
    ) {
        var ordinal = item.ordinal();
        if (ordinal == null) {
            return;
        }
        if (ordinal >= 1 && ordinal <= maxOrdinal) {
            var target = query.items().get(ordinal - 1);
            if (target instanceof ExprSelectItem) {
                return;
            }
            context.addProblem(
                ValidationProblem.Code.GROUP_BY_INVALID_ORDINAL,
                "GROUP BY ordinal " + ordinal + " must reference expression select item",
                item,
                "group_by"
            );
            return;
        }
        context.addProblem(
            ValidationProblem.Code.GROUP_BY_INVALID_ORDINAL,
            "GROUP BY ordinal " + ordinal + " is out of range 1.." + maxOrdinal,
            item,
            "group_by"
        );
    }
}
