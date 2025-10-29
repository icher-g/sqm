package io.sqm.core.internal;

import io.sqm.core.Expression;
import io.sqm.core.UnaryPredicate;

/**
 * Represents an unary predicate.
 * <p>For example:</p>
 * <pre>
 *     {@code
 *     WHERE true
 *     WHERE active
 *     }
 * </pre>
 *
 * @param expr a boolean expression: TRUE, FALSE or a boolean column.
 */
public record UnaryPredicateImpl(Expression expr) implements UnaryPredicate {
}
