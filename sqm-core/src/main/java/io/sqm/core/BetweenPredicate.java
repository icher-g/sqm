package io.sqm.core;

import io.sqm.core.internal.BetweenPredicateImpl;

/**
 * Represents a BETWEEN statement.
 * <p>For example:</p>
 * <pre>
 *     {@code
 *     5 BETWEEN 1 AND 10;
 *     5 BETWEEN SYMMETRIC 10 AND 1;
 *     }
 * </pre>
 */
public non-sealed interface BetweenPredicate extends Predicate {

    /**
     * Creates a BETWEEN operator.
     *
     * @param value     a value.
     * @param lower     a lower boundary.
     * @param upper     an upper boundary.
     * @param symmetric indicates whether the order of the boundaries matters or not. True means the boundaries do not matter.
     * @return a new instance of BETWEEN operator.
     */
    static BetweenPredicate of(Expression value, Expression lower, Expression upper, boolean symmetric) {
        return new BetweenPredicateImpl(value, lower, upper, symmetric);
    }

    /**
     * Gets a value to compare.
     *
     * @return an expression representing the value.
     */
    Expression value();

    /**
     * Gets an expression representing a low boundary.
     *
     * @return a low boundary of the predicate.
     */
    Expression lower();

    /**
     * Gets an expression representing an upper boundary.
     *
     * @return an upper boundary of the predicate.
     */
    Expression upper();

    /**
     * Indicates whether the order of the boundaries must be preserved or can be ignored.
     *
     * @return True if the order can be ignored and False otherwise.
     */
    boolean symmetric();

    /**
     * Adds symmetric indicator to the predicate.
     *
     * @param symmetric an indicator to add.
     * @return this.
     */
    default BetweenPredicate symmetric(boolean symmetric) {
        return new BetweenPredicateImpl(value(), lower(), upper(), symmetric);
    }
}
