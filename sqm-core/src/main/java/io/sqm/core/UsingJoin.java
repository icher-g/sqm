package io.sqm.core;

import io.sqm.core.internal.UsingJoinImpl;

import java.util.List;

/**
 * Represents a USING JOIN statement.
 * <p>Example:</p>
 * <pre>
 *     {@code
 *     SELECT *
 *     FROM orders
 *     JOIN customers USING (customer_id, region_id);
 *     }
 * </pre>
 */
public non-sealed interface UsingJoin extends Join {
    /**
     * Creates a cross join with the provided table.
     *
     * @param right        a table to join.
     * @param usingColumns a list of columns to be used for joining.
     * @return A newly created instance of CROSS JOIN with the provided table.
     */
    static UsingJoin of(TableRef right, String... usingColumns) {
        return new UsingJoinImpl(right, List.of(usingColumns));
    }

    /**
     * Creates a cross join with the provided table.
     *
     * @param right        a table to join.
     * @param usingColumns a list of columns to be used for joining.
     * @return A newly created instance of CROSS JOIN with the provided table.
     */
    static UsingJoin of(TableRef right, List<String> usingColumns) {
        return new UsingJoinImpl(right, usingColumns);
    }

    /**
     * USING (col1, col2, ...);
     */
    List<String> usingColumns();
}
