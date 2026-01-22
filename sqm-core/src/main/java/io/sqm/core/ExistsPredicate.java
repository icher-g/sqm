package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

/**
 * Represents an EXISTS predicate.
 * <p>For example:</p>
 * <pre>
 *     {@code
 *     SELECT *
 *     FROM customers c
 *     WHERE NOT EXISTS (
 *         SELECT 1
 *         FROM orders o
 *         WHERE o.customer_id = c.id
 *     );
 *     }
 * </pre>
 */
public non-sealed interface ExistsPredicate extends Predicate {

    /**
     * Creates EXISTS predicate.
     *
     * @param subquery a sub query used in the predicate.
     * @param negated  indicates whether this is EXISTS or NOT EXISTS predicate.
     * @return a new instance of EXISTS predicate.
     */
    static ExistsPredicate of(Query subquery, boolean negated) {
        return new Impl(subquery, negated);
    }

    /**
     * Gets a sub query used in the predicate.
     *
     * @return a sub query.
     */
    Query subquery();

    /**
     * Indicates whether this is EXISTS or NOT EXISTS predicate.
     *
     * @return True if this is NOT EXISTS predicate and False otherwise.
     */
    boolean negated();

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
        return v.visitExistsPredicate(this);
    }

    /**
     * A default implementation of a {@link ExistsPredicate}.
     * <p>For example:</p>
     * <pre>
     *     {@code
     *     SELECT *
     *     FROM customers c
     *     WHERE NOT EXISTS (
     *         SELECT 1
     *         FROM orders o
     *         WHERE o.customer_id = c.id
     *     );
     *     }
     * </pre>
     *
     * @param subquery a sub query
     * @param negated  indicates whether this is EXISTS or NOT EXISTS predicate. False means EXISTS.
     */
    record Impl(Query subquery, boolean negated) implements ExistsPredicate {
    }
}
