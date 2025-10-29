package io.sqm.core;

import io.sqm.core.internal.LikePredicateImpl;

/**
 * Represents LIKE '%abc%' predicate.
 * <p>Examples:</p>
 * <pre>
 *     {@code
 *     name LIKE 'A%'
 *     name NOT LIKE 'A%'
 *     path LIKE 'C:\\%' ESCAPE '\'
 *     }
 * </pre>
 *
 */
public non-sealed interface LikePredicate extends Predicate {

    /**
     * Creates a LIKE operator.
     *
     * @param value   an expression to compare.
     * @param pattern a pattern to use in the predicate.
     * @param escape  an escape expression is used. This value can be NULL.
     * @param negated indicates whether this is LIKE or NOT LIKE predicate.
     * @return a new instance of the LIKE operator.
     */
    static LikePredicate of(Expression value, Expression pattern, Expression escape, boolean negated) {
        return new LikePredicateImpl(value, pattern, escape, negated);
    }

    /**
     * Gets an expression to compare.
     *
     * @return an expression representing the value.
     */
    Expression value();

    /**
     * Gets a pattern to use in the predicate.
     *
     * @return an expression representing the pattern.
     */
    Expression pattern();

    /**
     * Gets an escape expression is used. This value can be NULL.
     *
     * @return an expression representing an escape expression.
     */
    Expression escape();

    /**
     * Indicates whether this is LIKE or NOT LIKE predicate.
     *
     * @return True if this is NOT LIKE predicate and False otherwise.
     */
    boolean negated();

    /**
     * Creates a new instance of the predicate with the escape expression.
     *
     * @param escape the escape expression to use.
     * @return this.
     */
    default LikePredicate escape(Expression escape) {
        return new LikePredicateImpl(value(), pattern(), escape, negated());
    }
}
