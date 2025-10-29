package io.sqm.core;

import io.sqm.core.internal.OrPredicateImpl;

/**
 * Represents an OR predicate.
 * <p>For example:</p>
 * <pre>
 *     {@code
 *     t.a > 10 OR t.b IN (1, 2, 3);
 *     }
 * </pre>
 */
public non-sealed interface OrPredicate extends CompositePredicate {

    /**
     * Creates OR predicate.
     *
     * @param lhs  lhs-hand-sided predicate.
     * @param rhs rhs-hand-sided predicate.
     * @return A newly created instance of the OR predicate.
     */
    static OrPredicate of(Predicate lhs, Predicate rhs) {
        return new OrPredicateImpl(lhs, rhs);
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
