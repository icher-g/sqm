package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.Objects;

/**
 * Represents a single item inside a {@link ResultClause}.
 * <p>
 * Structurally this is similar to a select expression with an optional alias, but it is modeled
 * separately because it belongs to mutation-result semantics rather than to a {@code SELECT} list.
 * <p>
 * Examples:
 * <ul>
 *     <li>{@code RETURNING id}</li>
 *     <li>{@code RETURNING now() AS ts}</li>
 *     <li>{@code OUTPUT INSERTED.id AS new_id}</li>
 * </ul>
 */
public non-sealed interface ExprResultItem extends ResultItem {

    /**
     * Creates a new {@link ResultItem}.
     *
     * @param expr returned expression. Must not be {@code null}.
     * @return a new {@link ResultItem}.
     */
    static ExprResultItem of(Expression expr) {
        return of(expr, null);
    }

    /**
     * Creates a new {@link ResultItem}.
     *
     * @param expr returned expression. Must not be {@code null}.
     * @param alias      optional alias. May be {@code null}.
     * @return a new {@link ResultItem}.
     */
    static ExprResultItem of(Expression expr, Identifier alias) {
        return new Impl(expr, alias);
    }

    /**
     * Returns the projected result expression.
     *
     * @return result expression
     */
    Expression expr();

    /**
     * Returns the optional result alias.
     *
     * @return alias identifier or {@code null}
     */
    Identifier alias();

    /**
     * Returns a new result item with the provided alias.
     *
     * @param alias alias identifier
     * @return aliased result item
     */
    default ResultItem as(Identifier alias) {
        return of(expr(), alias);
    }

    /**
     * Returns a new result item with the provided alias text.
     *
     * @param alias alias text
     * @return aliased result item
     */
    default ResultItem as(String alias) {
        return as(alias == null ? null : Identifier.of(alias));
    }

    /**
     * Accepts a visitor.
     *
     * @param v   visitor instance
     * @param <R> result type
     * @return visitor result
     */
    @Override
    default <R> R accept(NodeVisitor<R> v) {
        return v.visitExprResultItem(this);
    }

    /**
     * Default immutable implementation.
     *
     * @param expr projected result expression
     * @param alias      optional alias
     */
    record Impl(Expression expr, Identifier alias) implements ExprResultItem {

        /**
         * Creates a result item implementation.
         *
         * @param expr projected result expression
         * @param alias      optional alias
         */
        public Impl {
            Objects.requireNonNull(expr, "expr");
        }
    }
}
