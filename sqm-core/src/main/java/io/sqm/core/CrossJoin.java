package io.sqm.core;

import io.sqm.core.internal.CrossJoinImpl;
import io.sqm.core.walk.NodeVisitor;

/**
 * Represents a CROSS JOIN. This produces the Cartesian product of both tables.
 * <p>Example:</p>
 * <pre>
 *     {@code
 *     SELECT *
 *     FROM employees
 *     CROSS JOIN departments;
 *     }
 * </pre>
 */
public non-sealed interface CrossJoin extends Join {
    /**
     * Creates a cross join with the provided table.
     *
     * @param right a table to join.
     * @return A newly created instance of CROSS JOIN with the provided table.
     */
    static CrossJoin of(TableRef right) {
        return new CrossJoinImpl(right);
    }

    /**
     * Creates cross join with the provided table name.
     *
     * @param table the name of the table. This is not qualified name.
     * @return A newly created instance of the table.
     */
    static CrossJoin of(String table) {
        return new CrossJoinImpl(TableRef.table(table));
    }

    /**
     * Creates cross join with the provided table schema and name.
     *
     * @param schema a table schema.
     * @param table  the name of the table. This is not qualified name.
     * @return A newly created instance of the table.
     */
    static CrossJoin of(String schema, String table) {
        return new CrossJoinImpl(TableRef.table(schema, table));
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
        return v.visitCrossJoin(this);
    }
}
