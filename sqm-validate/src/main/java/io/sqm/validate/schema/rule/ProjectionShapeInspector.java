package io.sqm.validate.schema.rule;

import io.sqm.core.Expression;
import io.sqm.core.Query;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import io.sqm.validate.schema.model.DbType;

import java.util.List;
import java.util.Optional;

/**
 * Provides query projection shape/type inspection used by schema validation rules.
 */
interface ProjectionShapeInspector {
    /**
     * Returns whether query projects exactly one expression column.
     *
     * @param query query to inspect.
     * @return true when query shape is single expression projection.
     */
    boolean isSingleExpressionProjection(Query query);

    /**
     * Returns expression projection types for the query when shape is expression-only.
     *
     * @param query query to inspect.
     * @param context validation context for type inference; may be null for shape-only checks.
     * @return projected expression types.
     */
    Optional<List<Optional<DbType>>> expressionProjectionTypes(Query query, SchemaValidationContext context);

    /**
     * Returns projection arity for query when determinable.
     *
     * @param query query to inspect.
     * @return projection arity.
     */
    Optional<Integer> projectionArity(Query query);

    /**
     * Returns expression projection values when query projection contains only expression items.
     *
     * @param query query to inspect.
     * @return projected expressions.
     */
    Optional<List<Expression>> expressionProjectionExpressions(Query query);
}
