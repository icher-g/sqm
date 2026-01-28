package io.sqm.core;

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
     * @param kind         a join kind.
     * @param usingColumns a list of columns to be used for joining.
     * @return A newly created instance of CROSS JOIN with the provided table.
     */
    static UsingJoin of(TableRef right, JoinKind kind, String... usingColumns) {
        return new Impl(right, kind, List.of(usingColumns));
    }

    /**
     * Creates a cross join with the provided table.
     *
     * @param right        a table to join.
     * @param kind         a join kind.
     * @param usingColumns a list of columns to be used for joining.
     * @return A newly created instance of CROSS JOIN with the provided table.
     */
    static UsingJoin of(TableRef right, JoinKind kind, List<String> usingColumns) {
        return new Impl(right, kind, usingColumns);
    }

    /**
     * Gets the join type.
     *
     * @return the join type.
     */
    JoinKind kind();

    /**
     * USING (col1, col2, ...);
     */
    List<String> usingColumns();

    /**
     * Adds a kind to the current join instance.
     *
     * @param kind a kind to add.
     * @return A newly created instance with the provided kind.
     */
    default UsingJoin ofKind(JoinKind kind) {
        return new Impl(right(), kind, usingColumns());
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
        return v.visitUsingJoin(this);
    }

    /**
     * Implements a USING JOIN.
     * A USING join is a shorthand syntax for joining tables when the join column(s) have the same name in both tables.
     * <p>Example:</p>
     * <pre>
     *     {@code
     *     SELECT *
     *     FROM employees
     *     JOIN departments USING (department_id);
     *     }
     * </pre>
     *
     * @param right        the table to join.
     * @param kind         a join kind.
     * @param usingColumns a list of columns to join on.
     */
    record Impl(TableRef right, JoinKind kind, List<String> usingColumns) implements UsingJoin {
    }
}
