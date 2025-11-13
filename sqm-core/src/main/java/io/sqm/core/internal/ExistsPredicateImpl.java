package io.sqm.core.internal;

import io.sqm.core.ExistsPredicate;
import io.sqm.core.Query;

/**
 * A default implementation of a {@link ExistsPredicate}.
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
 *
 * @param subquery a sub query
 * @param negated  indicates whether this is EXISTS or NOT EXISTS predicate. False means EXISTS.
 */
public record ExistsPredicateImpl(Query subquery, boolean negated) implements ExistsPredicate {
}
