package io.sqm.core;

import io.sqm.core.internal.IsNullPredicateImpl;

/**
 * Represents IS NULL / IS NOT NULL predicates.
 */
public non-sealed interface IsNullPredicate extends Predicate {

    /**
     * Creates an IS NULL / IS NOT NULL predicate.
     *
     * @param expr    an expression to be checked for NULL / NOT NULL.
     * @param negated indicates whether this is NULL or NOT NULL predicate.
     * @return a new instance of the predicate.
     */
    static IsNullPredicate of(Expression expr, boolean negated) {
        return new IsNullPredicateImpl(expr, negated);
    }

    /**
     * Gets an expression to be checked for NULL / NOT NULL.
     *
     * @return an expression.
     */
    Expression expr();

    /**
     * Indicates whether this is NULL or NOT NULL predicate.
     *
     * @return True if this is NOT NULL predicate and False otherwise.
     */
    boolean negated();
}
