package io.sqm.core.internal;

import io.sqm.core.NotPredicate;
import io.sqm.core.Predicate;

/**
 * Implements a NOT predicate.
 * <p>Example:</p>
 * <pre>
 *     {@code
 *     NOT EXISTS(SELECT * FROM t WHERE t.name = 'a')
 *     }
 * </pre>
 */
public record NotPredicateImpl(Predicate inner) implements NotPredicate {
}
