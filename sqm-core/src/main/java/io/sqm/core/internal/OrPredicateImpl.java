package io.sqm.core.internal;

import io.sqm.core.OrPredicate;
import io.sqm.core.Predicate;

/**
 * Represents an OR predicate.
 * <p>For example:</p>
 * <pre>
 *     {@code
 *     t.a > 10 OR t.b IN (1, 2, 3);
 *     }
 * </pre>
 *
 * @param lhs  a left-hand-sided predicate.
 * @param rhs a right-hand-sided predicate.
 */
public record OrPredicateImpl(Predicate lhs, Predicate rhs) implements OrPredicate {
}
