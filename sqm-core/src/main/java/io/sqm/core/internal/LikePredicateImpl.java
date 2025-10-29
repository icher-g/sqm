package io.sqm.core.internal;

import io.sqm.core.Expression;
import io.sqm.core.LikePredicate;

/**
 * Implements LIKE '%abc%' predicate.
 * <p>Examples:</p>
 * <pre>
 *     {@code
 *     name LIKE 'A%'
 *     name NOT LIKE 'A%'
 *     path LIKE 'C:\\%' ESCAPE '\'
 *     }
 * </pre>
 *
 * @param value   an expression to compare.
 * @param pattern a pattern to use in the predicate.
 * @param escape  an escape expression is used. This value can be NULL.
 * @param negated indicates whether this is LIKE or NOT LIKE predicate.
 */
public record LikePredicateImpl(Expression value, Expression pattern, Expression escape, boolean negated) implements LikePredicate {
}
