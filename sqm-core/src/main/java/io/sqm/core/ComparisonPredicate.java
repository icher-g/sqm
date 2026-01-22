package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

/**
 * Represents a comparison predicate.
 * <p>Example:</p>
 * <pre>
 *     {@code
 *     WHERE t.a >= 1;
 *     WHERE t1.a = t2.b;
 *     WHERE t.c < 100;
 *     }
 * </pre>
 */
public non-sealed interface ComparisonPredicate extends Predicate {

    /**
     * Creates a comparison operator.
     *
     * @param lhs      a lhs side expression.
     * @param operator an operator.
     * @param rhs      a rhs side expression.
     * @return an instance of a comparison operator.
     */
    static ComparisonPredicate of(Expression lhs, ComparisonOperator operator, Expression rhs) {
        return new Impl(lhs, operator, rhs);
    }

    /**
     * Gets the left-hand-sided expression.
     *
     * @return an expression on the left side of the predicate.
     */
    Expression lhs();

    /**
     * Gets the comparison operator.
     *
     * @return {@link ComparisonOperator}.
     */
    ComparisonOperator operator();

    /**
     * Gets the right-hand-sided expression.
     *
     * @return an expression on the right side of the predicate.
     */
    Expression rhs();

    /**
     * Accepts a {@link NodeVisitor} and dispatches control to the
     * visitor method corresponding to the concrete subtype
     *
     * @param v   the visitor instance to accept (must not be {@code null})
     * @param <R> the result type returned by the visitor
     * @return the result produced by the visitor
     */
    @Override
    default <R> R accept(NodeVisitor<R> v) {
        return v.visitComparisonPredicate(this);
    }

    /**
     * Implements the comparison predicate.
     *
     * @param lhs      the left-hand-sided expression.
     * @param operator the comparison operator.
     * @param rhs      the right-hand-sided expression.
     */
    record Impl(Expression lhs, ComparisonOperator operator, Expression rhs) implements ComparisonPredicate {

        /**
         * This constructor validates that rhs is a {@link ValueSet}.
         *
         * @param lhs      a left-hand-sided expression.
         * @param operator a comparison operator.
         * @param rhs      a right-hand-sided expression.
         */
        public Impl {
            if (rhs instanceof ValueSet) {
                throw new IllegalArgumentException(operator + " operator cannot be applied to a list of values.");
            }
        }
    }
}
