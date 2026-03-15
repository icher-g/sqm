package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

/**
 * SQL row-limiting specification rendered near the {@code SELECT} keyword,
 * such as SQL Server {@code TOP}.
 */
public non-sealed interface TopSpec extends Node {

    /**
     * Creates a plain {@code TOP (<count>)} specification.
     *
     * @param count row count expression
     * @return top specification
     */
    static TopSpec of(Expression count) {
        return of(count, false, false);
    }

    /**
     * Creates a top specification.
     *
     * @param count row count expression
     * @param percent whether {@code PERCENT} is present
     * @param withTies whether {@code WITH TIES} is present
     * @return top specification
     */
    static TopSpec of(Expression count, boolean percent, boolean withTies) {
        return new Impl(count, percent, withTies);
    }

    /**
     * Returns the top-count expression.
     *
     * @return top-count expression
     */
    Expression count();

    /**
     * Indicates whether the top specification uses {@code PERCENT}.
     *
     * @return {@code true} when {@code PERCENT} is present
     */
    boolean percent();

    /**
     * Indicates whether the top specification uses {@code WITH TIES}.
     *
     * @return {@code true} when {@code WITH TIES} is present
     */
    boolean withTies();

    /**
     * Accepts a visitor.
     *
     * @param v visitor
     * @param <R> result type
     * @return visitor result
     */
    @Override
    default <R> R accept(NodeVisitor<R> v) {
        return v.visitTopSpec(this);
    }

    /**
     * Default immutable implementation.
     *
     * @param count top-count expression
     * @param percent whether {@code PERCENT} is present
     * @param withTies whether {@code WITH TIES} is present
     */
    record Impl(Expression count, boolean percent, boolean withTies) implements TopSpec {

        /**
         * Creates a top specification implementation.
         *
         * @param count top-count expression
         * @param percent whether {@code PERCENT} is present
         * @param withTies whether {@code WITH TIES} is present
         */
        public Impl {
            if (count == null) {
                throw new NullPointerException("count");
            }
        }
    }
}
