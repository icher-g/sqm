package io.sqm.core.internal;

import io.sqm.core.ComparisonOperator;
import io.sqm.core.ComparisonPredicate;
import io.sqm.core.Expression;
import io.sqm.core.ValueSet;

/**
 * Implements the comparison predicate.
 *
 * @param lhs      the left-hand-sided expression.
 * @param operator the comparison operator.
 * @param rhs      the right-hand-sided expression.
 */
public record ComparisonPredicateImpl(Expression lhs, ComparisonOperator operator, Expression rhs) implements ComparisonPredicate {

    /**
     * This constructor validates that rhs is a {@link ValueSet}.
     *
     * @param lhs      a left-hand-sided expression.
     * @param operator a comparison operator.
     * @param rhs      a right-hand-sided expression.
     */
    public ComparisonPredicateImpl {
        if (rhs instanceof ValueSet) {
            throw new IllegalArgumentException(operator + " operator cannot be applied to a list of values.");
        }
    }
}
