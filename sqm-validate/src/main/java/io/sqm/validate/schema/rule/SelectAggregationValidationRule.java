package io.sqm.validate.schema.rule;

import io.sqm.core.ColumnExpr;
import io.sqm.core.GroupItem;
import io.sqm.core.SelectQuery;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.function.FunctionCatalog;
import io.sqm.validate.schema.internal.SchemaValidationContext;

import java.util.HashSet;
import java.util.Set;

/**
 * Validates grouped SELECT and HAVING aggregation context constraints.
 */
final class SelectAggregationValidationRule implements SchemaValidationRule<SelectQuery> {
    private final AggregationAnalysis aggregationAnalysis;

    /**
     * Creates aggregation validation rule.
     *
     * @param functionCatalog function catalog used to detect aggregate functions.
     */
    SelectAggregationValidationRule(FunctionCatalog functionCatalog) {
        this.aggregationAnalysis = new AggregationAnalysis(functionCatalog);
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
     * Validates grouped projection and HAVING expression constraints.
     *
     * @param node select query node.
     * @param context schema validation context.
     */
    @Override
    public void validate(SelectQuery node, SchemaValidationContext context) {
        var groupedExpressions = groupedExpressions(node);
        var groupedColumnKeys = groupedColumnKeys(groupedExpressions);
        if (!groupedExpressions.isEmpty()) {
            validateGroupedSelectItems(node, groupedExpressions, groupedColumnKeys, context);
        }
        validateHaving(node, groupedExpressions, groupedColumnKeys, context);
    }

    private void validateGroupedSelectItems(
        SelectQuery node,
        Set<io.sqm.core.Expression> groupedExpressions,
        Set<String> groupedColumnKeys,
        SchemaValidationContext context
    ) {
        for (var item : node.items()) {
            switch (item) {
                case io.sqm.core.ExprSelectItem exprSelectItem -> {
                    var expr = exprSelectItem.expr();
                    if (aggregationAnalysis.containsAggregate(expr) || groupedExpressions.contains(expr)) {
                        continue;
                    }
                    var outsideAggregateColumns = aggregationAnalysis.nonAggregateColumnKeys(expr);
                    if (!outsideAggregateColumns.isEmpty() && groupedColumnKeys.containsAll(outsideAggregateColumns)) {
                        continue;
                    }
                    context.addProblem(
                        ValidationProblem.Code.AGGREGATION_MISUSE,
                        "Non-aggregated SELECT expression must be grouped: " + expr,
                        node,
                        "select"
                    );
                }
                case io.sqm.core.StarSelectItem ignored -> context.addProblem(
                    ValidationProblem.Code.AGGREGATION_MISUSE,
                    "SELECT * is not allowed together with GROUP BY without explicit aggregation",
                    node,
                    "select"
                );
                case io.sqm.core.QualifiedStarSelectItem ignored -> context.addProblem(
                    ValidationProblem.Code.AGGREGATION_MISUSE,
                    "SELECT * is not allowed together with GROUP BY without explicit aggregation",
                    node,
                    "select"
                );
                default -> {
                }
            }
        }
    }

    private void validateHaving(
        SelectQuery node,
        Set<io.sqm.core.Expression> groupedExpressions,
        Set<String> groupedColumnKeys,
        SchemaValidationContext context
    ) {
        if (node.having() == null) {
            return;
        }
        var hasGroupBy = !groupedExpressions.isEmpty();
        var hasAggregate = aggregationAnalysis.containsAggregate(node.having());
        if (!hasGroupBy && !hasAggregate) {
            context.addProblem(
                ValidationProblem.Code.AGGREGATION_MISUSE,
                "HAVING without GROUP BY must contain aggregate expressions",
                node,
                "having"
            );
            return;
        }

        var outsideAggregateColumns = aggregationAnalysis.nonAggregateColumnKeys(node.having());
        if (!outsideAggregateColumns.isEmpty() && !groupedColumnKeys.containsAll(outsideAggregateColumns)) {
            context.addProblem(
                ValidationProblem.Code.AGGREGATION_MISUSE,
                "HAVING references non-grouped columns outside aggregate context",
                node,
                "having"
            );
        }
    }

    private static Set<io.sqm.core.Expression> groupedExpressions(SelectQuery node) {
        var grouped = new HashSet<io.sqm.core.Expression>();
        if (node.groupBy() == null) {
            return grouped;
        }
        for (var item : node.groupBy().items()) {
            if (item instanceof GroupItem.SimpleGroupItem simple) {
                if (simple.expr() != null) {
                    grouped.add(simple.expr());
                } else if (simple.ordinal() != null) {
                    var idx = simple.ordinal() - 1;
                    if (idx >= 0 && idx < node.items().size() && node.items().get(idx) instanceof io.sqm.core.ExprSelectItem exprSelectItem) {
                        grouped.add(exprSelectItem.expr());
                    }
                }
            }
        }
        return grouped;
    }

    private static Set<String> groupedColumnKeys(Set<io.sqm.core.Expression> groupedExpressions) {
        var keys = new HashSet<String>();
        for (var expr : groupedExpressions) {
            if (expr instanceof ColumnExpr columnExpr) {
                keys.add(AggregationAnalysis.columnKey(columnExpr));
            }
        }
        return keys;
    }
}
