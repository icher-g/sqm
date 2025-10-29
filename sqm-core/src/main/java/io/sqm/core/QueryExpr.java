package io.sqm.core;

import io.sqm.core.internal.QueryExprImpl;

/**
 * Represents a sub query expression.
 * <p>For example:</p>
 * <pre>
 *     {@code
 *     SELECT *
 *     FROM (
 *       SELECT * FROM t
 *     )
 *     }
 * </pre>
 */
public non-sealed interface QueryExpr extends ValueSet {
    /**
     * Gets a sub query.
     *
     * @return a sub query.
     */
    Query subquery();

    /**
     * Creates a new instance of a query expression.
     *
     * @param subquery a sub query to wrap.
     * @return A newly created instance of a wrapped query.
     */
    static QueryExpr of(Query subquery) {
        return new QueryExprImpl(subquery);
    }
}
