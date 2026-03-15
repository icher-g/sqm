package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.Objects;

/**
 * One projected item inside a SQL Server DML {@code OUTPUT} clause.
 */
public non-sealed interface OutputItem extends Node {

    /**
     * Creates an output item without an alias.
     *
     * @param expression projected output expression
     * @return output item
     */
    static OutputItem of(Expression expression) {
        return of(expression, null);
    }

    /**
     * Creates an output item with an optional alias.
     *
     * @param expression projected output expression
     * @param alias optional alias identifier
     * @return output item
     */
    static OutputItem of(Expression expression, Identifier alias) {
        return new Impl(expression, alias);
    }

    /**
     * Returns the projected output expression.
     *
     * @return output expression
     */
    Expression expression();

    /**
     * Returns the optional output alias.
     *
     * @return alias identifier or {@code null}
     */
    Identifier alias();

    /**
     * Returns a new output item with the provided alias.
     *
     * @param alias alias identifier
     * @return aliased output item
     */
    default OutputItem as(Identifier alias) {
        return of(expression(), alias);
    }

    /**
     * Returns a new output item with the provided alias text.
     *
     * @param alias alias text
     * @return aliased output item
     */
    default OutputItem as(String alias) {
        return as(alias == null ? null : Identifier.of(alias));
    }

    /**
     * Accepts a visitor.
     *
     * @param v visitor instance
     * @param <R> result type
     * @return visitor result
     */
    @Override
    default <R> R accept(NodeVisitor<R> v) {
        return v.visitOutputItem(this);
    }

    /**
     * Default immutable implementation.
     *
     * @param expression projected output expression
     * @param alias optional alias
     */
    record Impl(Expression expression, Identifier alias) implements OutputItem {

        /**
         * Creates an output item implementation.
         *
         * @param expression projected output expression
         * @param alias optional alias
         */
        public Impl {
            Objects.requireNonNull(expression, "expression");
        }
    }
}
