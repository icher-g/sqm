package io.sqm.core;

import io.sqm.core.internal.NotPredicateImpl;
import io.sqm.core.walk.NodeVisitor;

/**
 * Represents a NOT predicate.
 * <p>Example:</p>
 * <pre>
 *     {@code
 *     NOT EXISTS(SELECT * FROM t WHERE t.name = 'a')
 *     }
 * </pre>
 */
public non-sealed interface NotPredicate extends Predicate {

    /**
     * Creates NOT predicate.
     *
     * @param inner an inner predicate to negate.
     * @return a new NOT predicate.
     */
    static NotPredicate of(Predicate inner) {
        return new NotPredicateImpl(inner);
    }

    /**
     * Get the negated predicate.
     *
     * @return an inner predicate.
     */
    Predicate inner();

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
        return v.visitNotPredicate(this);
    }
}
