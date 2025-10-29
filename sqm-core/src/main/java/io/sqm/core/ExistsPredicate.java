package io.sqm.core;

import io.sqm.core.internal.ExistsPredicateImpl;

/**
 * Represents an EXISTS predicate.
 * <p>For example:</p>
 * <pre>
 *     {@code
 *     SELECT *
 *     FROM customers c
 *     WHERE NOT EXISTS (
 *         SELECT 1
 *         FROM orders o
 *         WHERE o.customer_id = c.id
 *     );
 *     }
 * </pre>
 */
public non-sealed interface ExistsPredicate extends Predicate {

    /**
     * Creates EXISTS predicate.
     *
     * @param subquery a sub query used in the predicate.
     * @param negated  indicates whether this is EXISTS or NOT EXISTS predicate.
     * @return a new instance of EXISTS predicate.
     */
    static ExistsPredicate of(Query subquery, boolean negated) {
        return new ExistsPredicateImpl(subquery, negated);
    }

    /**
     * Gets a sub query used in the predicate.
     *
     * @return a sub query.
     */
    Query subquery();

    /**
     * Indicates whether this is EXISTS or NOT EXISTS predicate.
     *
     * @return True if this is NOT EXISTS predicate and False otherwise.
     */
    boolean negated();
}
