package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

/**
 * Represents an OR predicate.
 * <p>For example:</p>
 * <pre>
 *     {@code
 *     t.a > 10 OR t.b IN (1, 2, 3);
 *     }
 * </pre>
 */
public non-sealed interface OrPredicate extends CompositePredicate {

    /**
     * Creates OR predicate.
     *
     * @param lhs  lhs-hand-sided predicate.
     * @param rhs rhs-hand-sided predicate.
     * @return A newly created instance of the OR predicate.
     */
    static OrPredicate of(Predicate lhs, Predicate rhs) {
        return new Impl(lhs, rhs);
    }

    /**
     * Gets a left-hand-sided predicate.
     *
     * @return a predicate on the left side of the expression.
     */
    Predicate lhs();

    /**
     * Gets a right-hand-sided predicate.
     *
     * @return a predicate on the right side of the expression.
     */
    Predicate rhs();

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
        return v.visitOrPredicate(this);
    }

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
    record Impl(Predicate lhs, Predicate rhs) implements OrPredicate {
    }
}
