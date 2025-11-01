package io.sqm.core;

import io.sqm.core.internal.NaturalJoinImpl;
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
        return new NaturalJoinImpl(right);
    }

    /**
     * Creates cross join with the provided table name.
     *
     * @param table the name of the table. This is not qualified name.
     * @return A newly created instance of the table.
     */
    static NaturalJoin of(String table) {
        return new NaturalJoinImpl(TableRef.table(table));
    }

    /**
     * Creates cross join with the provided table schema and name.
     *
     * @param schema a table schema.
     * @param table  the name of the table. This is not qualified name.
     * @return A newly created instance of the table.
     */
    static NaturalJoin of(String schema, String table) {
        return new NaturalJoinImpl(TableRef.table(schema, table));
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
}
