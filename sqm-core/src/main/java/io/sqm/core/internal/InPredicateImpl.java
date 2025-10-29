package io.sqm.core.internal;

import io.sqm.core.Expression;
import io.sqm.core.InPredicate;
import io.sqm.core.ValueSet;

/**
 * Implements an IN / NOT IN predicates.
 *
 * @param lhs     a left-hand-sided expression of the predicate.
 * @param rhs     a values set on the right side of the predicate.
 * @param negated indicates whether this predicate represents IN or NOT IN expression.
 */
public record InPredicateImpl(Expression lhs, ValueSet rhs, boolean negated) implements InPredicate {
}
