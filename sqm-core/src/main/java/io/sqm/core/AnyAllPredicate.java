package io.sqm.core;

import io.sqm.core.internal.AnyAllPredicateImpl;

/**
 * Represents ANY / ALL predicates.
 * <p>For example:</p>
 * <pre>
 *     {@code
 *     age <= ANY (SELECT age FROM users WHERE active = true)
 *     }
 * </pre>
 */
public non-sealed interface AnyAllPredicate extends Predicate {

    /**
     * Creates an ANY / ALL predicate.
     *
     * @param lhs        he left-hand-side of the predicate.
     * @param operator   a comparison operator.
     * @param subquery   a sub query that provides a set of values to compare against.
     * @param quantifier indicates what type of predicate is this: ANY or ALL.
     * @return a new instance of the predicate.
     */
    static AnyAllPredicate of(Expression lhs, ComparisonOperator operator, Query subquery, Quantifier quantifier) {
        return new AnyAllPredicateImpl(lhs, operator, subquery, quantifier);
    }

    /**
     * Gets the left-hand-side of the predicate.
     *
     * @return an expression representing a left-hand-side
     */
    Expression lhs();

    /**
     * Gets a comparison operator.
     *
     * @return {@link ComparisonOperator}.
     */
    ComparisonOperator operator();

    /**
     * Gets a sub query that provides a set of values to compare against.
     *
     * @return a sub query.
     */
    Query subquery();

    /**
     * Indicates what type of predicate is this: ANY or ALL.
     *
     * @return {@link Quantifier}.
     */
    Quantifier quantifier();
}
