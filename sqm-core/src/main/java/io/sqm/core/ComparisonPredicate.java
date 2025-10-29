package io.sqm.core;

import io.sqm.core.internal.ComparisonPredicateImpl;

/**
 * Represents a comparison predicate.
 * <p>Example:</p>
 * <pre>
 *     {@code
 *     WHERE t.a >= 1;
 *     WHERE t1.a = t2.b;
 *     WHERE t.c < 100;
 *     }
 * </pre>
 */
public non-sealed interface ComparisonPredicate extends Predicate {

    /**
     * Creates a comparison operator.
     *
     * @param left     a left side expression.
     * @param operator an operator.
     * @param right    a right side expression.
     * @return an instance of a comparison operator.
     */
    static ComparisonPredicate of(Expression left, ComparisonOperator operator, Expression right) {
        return new ComparisonPredicateImpl(left, operator, right);
    }

    /**
     * Gets the left-hand-sided expression.
     *
     * @return an expression on the left side of the predicate.
     */
    Expression lhs();

    /**
     * Gets the comparison operator.
     *
     * @return {@link ComparisonOperator}.
     */
    ComparisonOperator operator();

    /**
     * Gets the right-hand-sided expression.
     *
     * @return an expression on the right side of the predicate.
     */
    Expression rhs();
}
