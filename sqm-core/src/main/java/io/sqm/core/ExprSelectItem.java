package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

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
        return new Impl(expr, null);
    }

    /**
     * Creates an expression wrapper for a SELECT statement preserving alias quote metadata.
     *
     * @param expr  an expression to wrap.
     * @param alias an alias identifier or {@code null}.
     * @return a newly created instance of a wrapper.
     */
    static ExprSelectItem of(Expression expr, Identifier alias) {
        return new Impl(expr, alias);
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
    Identifier alias();

    /**
     * Adds an alias to a SELECT item.
     *
     * @param alias an alias to add.
     * @return A newly created instance with the provided alias. The {@link ExprSelectItem#expr()} field is preserved.
     */
    default ExprSelectItem as(String alias) {
        return as(alias == null ? null : Identifier.of(alias));
    }

    /**
     * Adds an alias to a SELECT item preserving quote metadata.
     *
     * @param alias an alias identifier to add.
     * @return A newly created instance with the provided alias. The {@link ExprSelectItem#expr()} field is preserved.
     */
    default ExprSelectItem as(Identifier alias) {
        return new Impl(expr(), alias);
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
        return v.visitExprSelectItem(this);
    }

    /**
     * Represents a wrapped expression in a SELECT statement.
     * <p>For example:</p>
     * <pre>
     *     {@code
     *     SELECT products.name AS name -- ExprSelectItem(ColumnRef("products", "name"), "name")
     *     }
     * </pre>
     *
     * @param expr an expression to wrap.
     * @param alias an alias identifier.
     */
    record Impl(Expression expr, Identifier alias) implements ExprSelectItem {
    }
}
