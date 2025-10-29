package io.sqm.core;

import io.sqm.core.internal.AndPredicateImpl;

/**
 * Represents an AND predicate.
 * <p>For example:</p>
 * <pre>
 *     {@code
 *     t.a > 10 AND t.b IN (1, 2, 3);
 *     }
 * </pre>
 */
public non-sealed interface AndPredicate extends CompositePredicate {

    /**
     * Creates AND predicate.
     *
     * @param lhs  lhs-hand-sided predicate.
     * @param rhs rhs-hand-sided predicate.
     * @return A newly created instance of the AND predicate.
     */
    static AndPredicate of(Predicate lhs, Predicate rhs) {
        return new AndPredicateImpl(lhs, rhs);
    }

    /**
     * Gets a left-hand-sided predicate.
     *
     * @return a predicate on the left side of the expression.
     */
    Predicate lhs();

    /**
     * Gets a right-hand-sided predicate.
     *
     * @return a predicate on the right side of the expression.
     */
    Predicate rhs();
}
