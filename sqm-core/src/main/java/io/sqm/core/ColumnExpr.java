package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

/**
 * Represents a column reference used in WHERE / JOIN / GROUP BY / ORDER BY etc.
 * <p>For example:</p>
 * <pre>
 *     {@code
 *     ON u.id = o.user_id;
 *     WHERE u.age > 18;
 *     GROUP BY u.country;
 *     HAVING COUNT(u.id) > 10;
 *     ORDER BY u.name;
 *     }
 * </pre>
 */
public non-sealed interface ColumnExpr extends Expression {

    /**
     * Creates a column reference expression with quote-aware identifiers.
     *
     * @param tableAlias a table alias identifier. Can be {@code null}.
     * @param name       a column name identifier.
     * @return a newly created instance of the column reference.
     */
    static ColumnExpr of(Identifier tableAlias, Identifier name) {
        return new Impl(tableAlias, name);
    }

    /**
     * Gets a table name/alias. Can be NULL if the table is not used.
     *
     * @return a table name/alias if exists or NULL otherwise.
     */
    Identifier tableAlias();

    /**
     * Gets the column name identifier with quote metadata.
     *
     * @return column name identifier.
     */
    Identifier name();

    /**
     * Adds a table name/alias to the column.
     *
     * @param tableAlias table name or alias.
     * @return this.
     */
    default ColumnExpr inTable(String tableAlias) {
        return new Impl(tableAlias == null ? null : Identifier.of(tableAlias), name());
    }

    /**
     * Adds a table name/alias identifier to the column.
     *
     * @param tableAlias table name or alias identifier.
     * @return a new instance with the provided table alias.
     */
    default ColumnExpr inTable(Identifier tableAlias) {
        return new Impl(tableAlias, name());
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
        return v.visitColumnExpr(this);
    }

    /**
     * Implements a column reference used in WHERE / JOIN / GROUP BY / ORDER BY etc.
     * <p>For example:</p>
     * <pre>
     *     {@code
     *     ON u.id = o.user_id;
     *     WHERE u.age > 18;
     *     GROUP BY u.country;
     *     HAVING COUNT(u.id) > 10;
     *     ORDER BY u.name;
     *     }
     * </pre>
     *
     * @param tableAlias a table name/alias identifier. Can be NULL if the table is not used.
     * @param name       the name of the column identifier.
     */
    record Impl(Identifier tableAlias, Identifier name) implements ColumnExpr {
        /**
         * Creates a column reference implementation.
         *
         * @param tableAlias optional table alias identifier
         * @param name       column name identifier
         */
        public Impl {
            java.util.Objects.requireNonNull(name, "name");
        }
    }
}
