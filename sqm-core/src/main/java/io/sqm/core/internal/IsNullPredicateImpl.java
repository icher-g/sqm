package io.sqm.core.internal;

import io.sqm.core.Expression;
import io.sqm.core.IsNullPredicate;

/**
 * Implements IS NULL / IS NOT NULL predicates.
 *
 * @param expr    an expression to be checked for NULL / NOT NULL.
 * @param negated indicates whether this is NULL or NOT NULL predicate.
 */
public record IsNullPredicateImpl(Expression expr, boolean negated) implements IsNullPredicate {
}
