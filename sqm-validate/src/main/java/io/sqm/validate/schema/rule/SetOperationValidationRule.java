package io.sqm.validate.schema.rule;

import io.sqm.core.CompositeQuery;
import io.sqm.core.ColumnExpr;
import io.sqm.core.ExprSelectItem;
import io.sqm.core.Query;
import io.sqm.core.SelectQuery;
import io.sqm.core.WithQuery;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import io.sqm.catalog.model.CatalogType;
import io.sqm.validate.schema.model.CatalogTypeSemantics;

import java.util.List;
import java.util.Optional;

/**
 * Validates set-operation term shape and compatible projected types.
 */
final class SetOperationValidationRule implements SchemaValidationRule<CompositeQuery> {
    private final ProjectionShapeInspector projectionShapeInspector;

    /**
     * Creates set-operation validation rule.
     *
     * @param projectionShapeInspector projection shape inspector.
     */
    SetOperationValidationRule(ProjectionShapeInspector projectionShapeInspector) {
        this.projectionShapeInspector = projectionShapeInspector;
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
     * Validates column counts and projected type compatibility per term position.
     *
     * @param node composite query node.
     * @param context schema validation context.
     */
    @Override
    public void validate(CompositeQuery node, SchemaValidationContext context) {
        if (node.terms().size() < 2) {
            return;
        }

        for (int i = 1; i < node.terms().size(); i++) {
            var leftTypes = projectionShapeInspector.expressionProjectionTypes(node.terms().get(i - 1), context);
            var rightTypes = projectionShapeInspector.expressionProjectionTypes(node.terms().get(i), context);
            if (leftTypes.isEmpty() || rightTypes.isEmpty()) {
                continue;
            }

            var left = leftTypes.get();
            var right = rightTypes.get();
            if (left.size() != right.size()) {
                context.addProblem(
                    ValidationProblem.Code.SET_OPERATION_COLUMN_COUNT_MISMATCH,
                    "Set operation term column count mismatch between term " + i + " and term " + (i + 1),
                    node,
                    "set_operation"
                );
                continue;
            }

            validateTypes(left, right, i, node, context);
        }

        validateOrderByExpressions(node, context, projectionShapeInspector);
    }

    private static void validateTypes(
        List<Optional<CatalogType>> left,
        List<Optional<CatalogType>> right,
        int pairIndex,
        CompositeQuery node,
        SchemaValidationContext context
    ) {
        for (int c = 0; c < left.size(); c++) {
            if (left.get(c).isPresent() && right.get(c).isPresent()
                && !CatalogTypeSemantics.comparable(left.get(c).get(), right.get(c).get())) {
                context.addProblem(
                    ValidationProblem.Code.TYPE_MISMATCH,
                    "Set operation type mismatch at term pair "
                        + pairIndex
                        + "/"
                        + (pairIndex + 1)
                        + ", column "
                        + (c + 1)
                        + ": "
                        + left.get(c).get()
                        + " vs "
                        + right.get(c).get(),
                    node,
                    "set_operation"
                );
            }
        }
    }

    /**
     * Validates that expression-based ORDER BY items in set operations reference
     * projected result expressions.
     *
     * @param node composite query node.
     * @param context validation context.
     */
    private static void validateOrderByExpressions(
        CompositeQuery node,
        SchemaValidationContext context,
        ProjectionShapeInspector projectionShapeInspector
    ) {
        if (node.orderBy() == null || node.orderBy().items().isEmpty()) {
            return;
        }
        var projections = projectionShapeInspector.expressionProjectionExpressions(node);
        if (projections.isEmpty()) {
            for (var item : node.orderBy().items()) {
                if (item.ordinal() != null || item.expr() == null) {
                    continue;
                }
                context.addProblem(
                    ValidationProblem.Code.SET_OPERATION_ORDER_BY_INVALID,
                    "Set operation ORDER BY expressions require expression-only result shape; use ordinals instead",
                    item,
                    "set_operation.order_by"
                );
            }
            return;
        }
        for (var item : node.orderBy().items()) {
            if (item.ordinal() != null || item.expr() == null) {
                continue;
            }
            if (projections.get().contains(item.expr())) {
                continue;
            }
            if (item.expr() instanceof ColumnExpr column && column.tableAlias() == null && isProjectedName(node, column)) {
                continue;
            }
            context.addProblem(
                ValidationProblem.Code.SET_OPERATION_ORDER_BY_INVALID,
                "Set operation ORDER BY expression must reference projected result expression",
                item,
                "set_operation.order_by"
            );
        }
    }

    private static boolean isProjectedName(Query query, ColumnExpr column) {
        return switch (query) {
            case CompositeQuery composite -> isProjectedName(composite.terms().getFirst(), column);
            case WithQuery with -> isProjectedName(with.body(), column);
            case SelectQuery select -> select.items().stream()
                .filter(ExprSelectItem.class::isInstance)
                .map(ExprSelectItem.class::cast)
                .filter(item -> {
                    var name = item.alias() != null
                        ? item.alias()
                        : item.expr() instanceof ColumnExpr projected ? projected.name() : null;
                    return name != null && name.value().equalsIgnoreCase(column.name().value());
                }).count() == 1;
            default -> false;
        };
    }
}


