package io.sqm.validate.schema.rule;

import io.sqm.core.*;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import io.sqm.validate.schema.model.DbType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Default {@link ProjectionShapeInspector} implementation.
 */
class DefaultProjectionShapeInspector implements ProjectionShapeInspector {
    /**
     * Returns whether query projects exactly one expression column.
     *
     * @param query query to inspect.
     * @return true when query shape is single expression projection.
     */
    @Override
    public boolean isSingleExpressionProjection(Query query) {
        return expressionProjectionTypes(query, null).map(types -> types.size() == 1).orElse(false);
    }

    /**
     * Returns expression projection types for the query when shape is expression-only.
     *
     * @param query query to inspect.
     * @param context validation context for type inference; may be null for shape-only checks.
     * @return projected expression types.
     */
    @Override
    public Optional<List<Optional<DbType>>> expressionProjectionTypes(
        Query query,
        SchemaValidationContext context
    ) {
        return switch (query) {
            case SelectQuery select -> selectProjectionTypes(select, context);
            case WithQuery with -> expressionProjectionTypes(with.body(), context);
            case CompositeQuery composite -> expressionProjectionTypesForComposite(composite, context);
            default -> Optional.empty();
        };
    }

    /**
     * Returns projection arity for query when determinable.
     *
     * @param query query to inspect.
     * @return projection arity.
     */
    @Override
    public Optional<Integer> projectionArity(Query query) {
        return switch (query) {
            case SelectQuery select -> Optional.of(select.items().size());
            case WithQuery with -> projectionArity(with.body());
            case CompositeQuery composite -> composite.terms().isEmpty()
                ? Optional.empty()
                : projectionArity(composite.terms().getFirst());
            default -> Optional.empty();
        };
    }

    /**
     * Returns expression projection values when query projection contains only expression items.
     *
     * @param query query to inspect.
     * @return projected expressions.
     */
    @Override
    public Optional<List<Expression>> expressionProjectionExpressions(Query query) {
        return switch (query) {
            case SelectQuery select -> selectProjectionExpressions(select);
            case WithQuery with -> expressionProjectionExpressions(with.body());
            case CompositeQuery composite -> composite.terms().isEmpty()
                ? Optional.empty()
                : expressionProjectionExpressions(composite.terms().getFirst());
            default -> Optional.empty();
        };
    }

    /**
     * Returns expression projection types for the first composite term.
     *
     * @param composite composite query.
     * @param context validation context.
     * @return projected expression types.
     */
    private Optional<List<Optional<DbType>>> expressionProjectionTypesForComposite(
        CompositeQuery composite,
        SchemaValidationContext context
    ) {
        if (composite.terms().isEmpty()) {
            return Optional.empty();
        }
        return expressionProjectionTypes(composite.terms().getFirst(), context);
    }

    /**
     * Returns select projection types when all select items are expression items.
     *
     * @param select select query.
     * @param context validation context.
     * @return projected expression types.
     */
    private Optional<List<Optional<DbType>>> selectProjectionTypes(
        SelectQuery select,
        SchemaValidationContext context
    ) {
        if (context == null) {
            var unknownTypes = new ArrayList<Optional<DbType>>(select.items().size());
            for (var item : select.items()) {
                if (!(item instanceof ExprSelectItem)) {
                    return Optional.empty();
                }
                unknownTypes.add(Optional.empty());
            }
            return Optional.of(List.copyOf(unknownTypes));
        }
        return context.inferProjectionTypes(select);
    }

    /**
     * Returns projected expressions when all select items are expression items.
     *
     * @param select select query.
     * @return projected expressions.
     */
    private Optional<List<Expression>> selectProjectionExpressions(SelectQuery select) {
        var expressions = new ArrayList<Expression>(select.items().size());
        for (var item : select.items()) {
            if (!(item instanceof ExprSelectItem exprItem)) {
                return Optional.empty();
            }
            expressions.add(exprItem.expr());
        }
        return Optional.of(List.copyOf(expressions));
    }
}
