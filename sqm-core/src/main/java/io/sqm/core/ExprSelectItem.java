package io.sqm.core;

import io.sqm.core.internal.ExprSelectItemImpl;

/**
 * Represents a wrapped expression in a SELECT statement.
 * <p>For example:</p>
 * <pre>
 *     {@code
 *     SELECT products.name AS name -- ExprSelectItem(ColumnRef("products", "name"), "name")
 *     }
 * </pre>
 */
public non-sealed interface ExprSelectItem extends SelectItem {

    /**
     * Creates an expression wrapper for a SELECT statement.
     *
     * @param expr an expression to wrap.
     * @return A newly created instance of a wrapper.
     */
    static ExprSelectItem of(Expression expr) {
        return new ExprSelectItemImpl(expr, null);
    }

    /**
     * Gets an expression wrapped by this instance.
     *
     * @return an expression.
     */
    Expression expr();

    /**
     * Gets an alias.
     *
     * @return an alias.
     */
    String alias();

    /**
     * Adds an alias to a SELECT item.
     *
     * @param alias an alias to add.
     * @return A newly created instance with the provided alias. The {@link ExprSelectItem#expr()} field is preserved.
     */
    default ExprSelectItem as(String alias) {
        return new ExprSelectItemImpl(expr(), alias);
    }
}
