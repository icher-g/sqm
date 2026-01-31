package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

/**
 * Represents IS DISTINCT FROM / IS NOT DISTINCT FROM predicates (null-safe comparison).
 *
 * <p>
 * When negated is false, this predicate represents "left IS DISTINCT FROM right".
 * When negated is true, this predicate represents "left IS NOT DISTINCT FROM right".
 * </p>
 */
public non-sealed interface IsDistinctFromPredicate extends Predicate {

    /**
     * Creates an IS DISTINCT FROM / IS NOT DISTINCT FROM predicate.
     *
     * @param left    the left expression (must not be null).
     * @param right   the right expression (must not be null).
     * @param negated indicates whether this is 'IS DISTINCT FROM' (false) or IS NOT DISTINCT FROM (true).
     * @return a new instance of the predicate.
     */
    static IsDistinctFromPredicate of(Expression left, Expression right, boolean negated) {
        return new Impl(left, right, negated);
    }

    /**
     * Gets the left expression.
     *
     * @return the left expression.
     */
    Expression lhs();

    /**
     * Gets the right expression.
     *
     * @return the right expression.
     */
    Expression rhs();

    /**
     * Indicates whether this is 'IS DISTINCT FROM' or IS NOT DISTINCT FROM predicate.
     *
     * @return true for IS NOT DISTINCT FROM, false for IS DISTINCT FROM.
     */
    boolean negated();

    /**
     * Accepts a visitor and dispatches control to the visitor method corresponding
     * to this node type.
     *
     * @param v   the visitor instance to accept (must not be null).
     * @param <R> the result type returned by the visitor.
     * @return the result produced by the visitor.
     */
    @Override
    default <R> R accept(NodeVisitor<R> v) {
        return v.visitIsDistinctFromPredicate(this);
    }

    /**
     * Default implementation.
     *
     * @param lhs     left expression
     * @param rhs     right expression
     * @param negated whether predicate is negated
     */
    record Impl(Expression lhs, Expression rhs, boolean negated) implements IsDistinctFromPredicate {
    }
}
