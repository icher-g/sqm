package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

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
        return new Impl(subquery);
    }

    /**
     * Accepts a {@link NodeVisitor} and dispatches control to the
     * visitor method corresponding to the concrete subtype
     *
     * @param v   the visitor instance to accept (must not be {@code null})
     * @param <R> the result type returned by the visitor
     * @return the result produced by the visitor
     */
    @Override
    default <R> R accept(NodeVisitor<R> v) {
        return v.visitQueryExpr(this);
    }

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
     *
     * @param subquery a sub query wrapped as an expression.
     */
    record Impl(Query subquery) implements QueryExpr {
    }
}
