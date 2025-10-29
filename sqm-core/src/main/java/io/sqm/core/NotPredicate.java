package io.sqm.core;

import io.sqm.core.internal.NotPredicateImpl;

/**
 * Represents a NOT predicate.
 * <p>Example:</p>
 * <pre>
 *     {@code
 *     NOT EXISTS(SELECT * FROM t WHERE t.name = 'a')
 *     }
 * </pre>
 */
public non-sealed interface NotPredicate extends Predicate {

    /**
     * Creates NOT predicate.
     *
     * @param inner an inner predicate to negate.
     * @return a new NOT predicate.
     */
    static NotPredicate of(Predicate inner) {
        return new NotPredicateImpl(inner);
    }

    /**
     * Get the negated predicate.
     *
     * @return an inner predicate.
     */
    Predicate inner();
}
