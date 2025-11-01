package io.sqm.core;

import io.sqm.core.internal.ColumnExprImpl;
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
     * Creates a column reference expression.
     *
     * @param name a name of the column.
     * @return A newly created instance of the column reference.
     */
    static ColumnExpr of(String name) {
        return new ColumnExprImpl(null, name);
    }

    /**
     * Creates a column reference expression.
     *
     * @param name       a name of the column.
     * @param tableAlias a table name/alias. Can be NULL if the table is not used.
     * @return A newly created instance of the column reference.
     */
    static ColumnExpr of(String tableAlias, String name) {
        return new ColumnExprImpl(tableAlias, name);
    }

    /**
     * Gets a table name/alias. Can be NULL if the table is not used.
     *
     * @return a table name/alias if exists or NULL otherwise.
     */
    String tableAlias();

    /**
     * Gets the name of the column.
     *
     * @return a name of the column.
     */
    String name();

    /**
     * Adds a table name/alias to the column.
     *
     * @param tableAlias table name or alias.
     * @return this.
     */
    default ColumnExpr inTable(String tableAlias) {
        return new ColumnExprImpl(tableAlias, name());
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
}
