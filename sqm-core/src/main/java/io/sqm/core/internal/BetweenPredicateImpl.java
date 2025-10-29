package io.sqm.core.internal;

import io.sqm.core.BetweenPredicate;
import io.sqm.core.Expression;

/**
 * Implements a BETWEEN statement.
 * <p>For example:</p>
 * <pre>
 *     {@code
 *     5 BETWEEN 1 AND 10;
 *     5 BETWEEN SYMMETRIC 10 AND 1;
 *     }
 * </pre>
 *
 * @param value     a value to compare.
 * @param lower     an expression representing a low boundary.
 * @param upper     an expression representing an upper boundary.
 * @param symmetric indicates whether the order of the boundaries must be preserved or can be ignored.
 */
public record BetweenPredicateImpl(Expression value, Expression lower, Expression upper, boolean symmetric) implements BetweenPredicate {
}
