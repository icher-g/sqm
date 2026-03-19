package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.List;
import java.util.Objects;

/**
 * Represents a dialect-neutral clause that defines the result produced by a data-changing statement.
 * <p>
 * This clause can be used to model:
 * <ul>
 *     <li>PostgreSQL {@code RETURNING}</li>
 *     <li>MySQL {@code RETURNING}</li>
 *     <li>T-SQL {@code OUTPUT}</li>
 * </ul>
 * <p>
 * A result clause contains a list of returned items and may optionally redirect them into a target
 * table-like destination, as supported by T-SQL via {@code OUTPUT ... INTO}.
 */
public non-sealed interface ResultClause extends Node {

    /**
     * Creates a new {@link ResultClause}.
     *
     * @param items returned items. Must not be {@code null}.
     * @return a new {@link ResultClause}.
     */
    static ResultClause of(List<ResultItem> items) {
        return of(items, null);
    }

    /**
     * Creates a new {@link ResultClause}.
     *
     * @param items returned items. Must not be {@code null}.
     * @param into optional target for redirecting produced rows. May be {@code null}.
     * @return a new {@link ResultClause}.
     */
    static ResultClause of(List<ResultItem> items, ResultInto into) {
        return new Impl(items, into);
    }

    /**
     * Returns returned items of this clause.
     *
     * @return immutable list of returned items.
     */
    List<ResultItem> items();

    /**
     * Returns the optional target that receives produced rows.
     *
     * @return target clause or {@code null} when result rows are returned directly.
     */
    ResultInto into();

    /**
     * Accepts a visitor.
     *
     * @param v visitor instance
     * @param <R> result type
     * @return visitor result
     */
    @Override
    default <R> R accept(NodeVisitor<R> v) {
        return v.visitResultClause(this);
    }

    /**
     * Default immutable implementation.
     *
     * @param items projected result items
     * @param into optional result-into target
     */
    record Impl(List<ResultItem> items, ResultInto into) implements ResultClause {

        /**
         * Creates an result clause implementation.
         *
         * @param items projected result items
         * @param into optional result-into target
         */
        public Impl {
            items = List.copyOf(Objects.requireNonNull(items, "items"));
            if (items.isEmpty()) {
                throw new IllegalArgumentException("items must not be empty");
            }
        }
    }
}
