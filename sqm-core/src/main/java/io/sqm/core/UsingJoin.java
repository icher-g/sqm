package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.List;
import java.util.Objects;

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
     * Creates a USING join with the provided list of column identifiers.
     *
     * @param right a table to join.
     * @param kind a join kind.
     * @param usingColumns identifiers used in the {@code USING (...)} clause.
     * @return a newly created USING join.
     */
    static UsingJoin of(TableRef right, JoinKind kind, List<Identifier> usingColumns) {
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
     *
     * @return list of column names used in USING clause
     */
    List<Identifier> usingColumns();

    /**
     * USING (col1, col2, ...) column names as plain values.
     *
     * @return list of identifier values used in USING clause
     */
    default List<String> usingColumnNames() {
        return usingColumns().stream().map(Identifier::value).toList();
    }

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
    record Impl(TableRef right, JoinKind kind, List<Identifier> usingColumns) implements UsingJoin {
        /**
         * Creates a USING join implementation.
         *
         * @param right the table to join
         * @param kind the join kind
         * @param usingColumns identifiers used in the USING clause
         */
        public Impl {
            Objects.requireNonNull(right, "right");
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(usingColumns, "usingColumns");
            usingColumns = List.copyOf(usingColumns);
        }
    }
}
