package io.sqm.core;

import io.sqm.core.internal.IsNullPredicateImpl;
import io.sqm.core.walk.NodeVisitor;

/**
 * Represents IS NULL / IS NOT NULL predicates.
 */
public non-sealed interface IsNullPredicate extends Predicate {

    /**
     * Creates an IS NULL / IS NOT NULL predicate.
     *
     * @param expr    an expression to be checked for NULL / NOT NULL.
     * @param negated indicates whether this is NULL or NOT NULL predicate.
     * @return a new instance of the predicate.
     */
    static IsNullPredicate of(Expression expr, boolean negated) {
        return new IsNullPredicateImpl(expr, negated);
    }

    /**
     * Gets an expression to be checked for NULL / NOT NULL.
     *
     * @return an expression.
     */
    Expression expr();

    /**
     * Indicates whether this is NULL or NOT NULL predicate.
     *
     * @return True if this is NOT NULL predicate and False otherwise.
     */
    boolean negated();

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
        return v.visitIsNullPredicate(this);
    }
}
