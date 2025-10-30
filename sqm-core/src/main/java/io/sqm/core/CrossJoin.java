package io.sqm.core;

import io.sqm.core.internal.CrossJoinImpl;

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
}
