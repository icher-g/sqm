package io.sqm.core;

import io.sqm.core.internal.InPredicateImpl;

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
        return new InPredicateImpl(lhs, rhs, negated);
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
}
