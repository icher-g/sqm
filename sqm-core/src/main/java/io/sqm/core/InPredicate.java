package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

/**
 * Represents an IN / NOT IN predicates.
 */
public non-sealed interface InPredicate extends Predicate {

    /**
     * Creates an IN / NOT IN predicate.
     *
     * @param lhs     a left-handed-side.
     * @param rhs     a right-handled-side
     * @param negated indicates if this is IN or NOT IN predicate. True means NOT IN.
     * @return a new instance of the IN / NOT IN predicate.
     */
    static InPredicate of(Expression lhs, ValueSet rhs, boolean negated) {
        return new Impl(lhs, rhs, negated);
    }

    /**
     * Gets a left-hand-sided expression of the predicate.
     *
     * @return an expression on the left side of the predicate.
     */
    Expression lhs();

    /**
     * Gets a values set on the right side of the predicate.
     *
     * @return a set of values.
     */
    ValueSet rhs();

    /**
     * Indicates whether this predicate represents IN or NOT IN expression.
     *
     * @return True if this is NOT IN predicate and False otherwise.
     */
    boolean negated();

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
        return v.visitInPredicate(this);
    }

    /**
     * Implements an IN / NOT IN predicates.
     *
     * @param lhs     a left-hand-sided expression of the predicate.
     * @param rhs     a values set on the right side of the predicate.
     * @param negated indicates whether this predicate represents IN or NOT IN expression.
     */
    record Impl(Expression lhs, ValueSet rhs, boolean negated) implements InPredicate {
    }
}
