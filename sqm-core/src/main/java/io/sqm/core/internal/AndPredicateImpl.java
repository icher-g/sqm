package io.sqm.core.internal;

import io.sqm.core.AndPredicate;
import io.sqm.core.Predicate;

/**
 * Represents an AND predicate.
 * <p>For example:</p>
 * <pre>
 *     {@code
 *     t.a > 10 AND t.b IN (1, 2, 3);
 *     }
 * </pre>
 *
 * @param lhs a left-hand-sided predicate.
 * @param rhs a right-hand-sided predicate.
 */
public record AndPredicateImpl(Predicate lhs, Predicate rhs) implements AndPredicate {
}
