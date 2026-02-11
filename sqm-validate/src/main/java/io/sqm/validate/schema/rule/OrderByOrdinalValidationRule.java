package io.sqm.validate.schema.rule;

import io.sqm.core.CompositeQuery;
import io.sqm.core.ExprSelectItem;
import io.sqm.core.OrderItem;
import io.sqm.core.SelectQuery;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;

/**
 * Validates ORDER BY ordinal bounds in {@link SelectQuery}.
 */
final class OrderByOrdinalValidationRule implements SchemaValidationRule<SelectQuery> {
    /**
     * Creates ORDER BY ordinal validation rule.
     */
    OrderByOrdinalValidationRule() {
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
     * Validates ORDER BY ordinals for SELECT query.
     *
     * @param node select query.
     * @param context validation context.
     */
    @Override
    public void validate(SelectQuery node, SchemaValidationContext context) {
        if (node.orderBy() == null) {
            return;
        }
        validateOrderItems(node, context);
    }

    /**
     * Validates ORDER BY items for select query, including ordinal target shape.
     *
     * @param node select query.
     * @param context validation context.
     */
    private static void validateOrderItems(SelectQuery node, SchemaValidationContext context) {
        var projectionArity = node.items().size();
        for (var item : node.orderBy().items()) {
            if (item.ordinal() == null) {
                continue;
            }
            if (item.ordinal() <= 0 || item.ordinal() > projectionArity) {
                context.addProblem(
                    ValidationProblem.Code.ORDER_BY_INVALID_ORDINAL,
                    "ORDER BY ordinal " + item.ordinal() + " is out of range 1.." + projectionArity,
                    item,
                    "order_by"
                );
                continue;
            }
            var target = node.items().get(item.ordinal() - 1);
            if (!(target instanceof ExprSelectItem)) {
                context.addProblem(
                    ValidationProblem.Code.ORDER_BY_INVALID_ORDINAL,
                    "ORDER BY ordinal " + item.ordinal() + " must reference expression select item",
                    item,
                    "order_by"
                );
            }
        }
    }
}

/**
 * Validates ORDER BY ordinal bounds in {@link CompositeQuery}.
 */
final class CompositeOrderByOrdinalValidationRule implements SchemaValidationRule<CompositeQuery> {
    private final ProjectionShapeInspector projectionShapeInspector;

    /**
     * Creates composite ORDER BY ordinal validation rule.
     *
     * @param projectionShapeInspector projection shape inspector.
     */
    CompositeOrderByOrdinalValidationRule(ProjectionShapeInspector projectionShapeInspector) {
        this.projectionShapeInspector = projectionShapeInspector;
    }

    /**
     * Validates ORDER BY ordinals for composite query.
     *
     * @param node composite query.
     * @param context validation context.
     */
    @Override
    public void validate(CompositeQuery node, SchemaValidationContext context) {
        if (node.orderBy() == null) {
            return;
        }
        var arity = projectionShapeInspector.projectionArity(node);
        if (arity.isEmpty()) {
            return;
        }
        validateOrderItems(node.orderBy().items(), arity.get(), context);
    }

    /**
     * Validates each ORDER BY item ordinal.
     *
     * @param items ORDER BY items.
     * @param projectionArity available projection size.
     * @param context validation context.
     */
    static void validateOrderItems(
        java.util.List<OrderItem> items,
        int projectionArity,
        SchemaValidationContext context
    ) {
        for (var item : items) {
            if (item.ordinal() == null) {
                continue;
            }
            if (item.ordinal() <= 0 || item.ordinal() > projectionArity) {
                context.addProblem(
                    ValidationProblem.Code.ORDER_BY_INVALID_ORDINAL,
                    "ORDER BY ordinal " + item.ordinal() + " is out of range 1.." + projectionArity,
                    item,
                    "order_by"
                );
            }
        }
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
}
