package io.sqm.core.internal;

import io.sqm.core.*;

/**
 * Implementation of the ANY / ALL predicates.
 * <p>For example:</p>
 * <pre>
 *     {@code
 *     age <= ANY (SELECT age FROM users WHERE active = true)
 *     }
 * </pre>
 *
 * @param lhs        the left-hand-side of the predicate.
 * @param operator   a comparison operator.
 * @param subquery   a sub query that provides a set of values to compare against.
 * @param quantifier indicates what type of predicate is this: ANY or ALL.
 */
public record AnyAllPredicateImpl(Expression lhs, ComparisonOperator operator, Query subquery, Quantifier quantifier) implements AnyAllPredicate {
}
