package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

/**
 * Implements a natural join.
 * A NATURAL JOIN is a type of join that automatically matches columns with the same name in both tables.
 * <p>Example:</p>
 * <pre>
 *     {@code
 *     SELECT *
 *     FROM employees
 *     NATURAL JOIN departments;
 *     }
 * </pre>
 */
public non-sealed interface NaturalJoin extends Join {
    /**
     * Creates a cross join with the provided table.
     *
     * @param right a table to join.
     * @return A newly created instance of CROSS JOIN with the provided table.
     */
    static NaturalJoin of(TableRef right) {
        return new Impl(right);
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
        return v.visitNaturalJoin(this);
    }

    /**
     * Implements a natural join.
     * A NATURAL JOIN is a type of join that automatically matches columns with the same name in both tables.
     * <p>Example:</p>
     * <pre>
     *     {@code
     *     SELECT *
     *     FROM employees
     *     NATURAL JOIN departments;
     *     }
     * </pre>
     *
     * @param right the right side of the join.
     */
    record Impl(TableRef right) implements NaturalJoin {
    }
}
