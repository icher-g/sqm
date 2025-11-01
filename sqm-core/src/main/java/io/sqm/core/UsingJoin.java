package io.sqm.core;

import io.sqm.core.internal.UsingJoinImpl;
import io.sqm.core.walk.NodeVisitor;

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
        return v.visitUsingJoin(this);
    }
}
