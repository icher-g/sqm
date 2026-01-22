package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

/**
 * Represents an unary predicate.
 * <p>For example:</p>
 * <pre>
 *     {@code
 *     WHERE true
 *     WHERE active
 *     }
 * </pre>
 */
public non-sealed interface UnaryPredicate extends Predicate {

    /**
     * Creates a unary predicate.
     *
     * @param expr a boolean expression: TRUE, FALSE or a boolean column.
     * @return a new instance of the unary predicate.
     */
    static UnaryPredicate of(Expression expr) {
        return new Impl(expr);
    }

    Expression expr();

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
        return v.visitUnaryPredicate(this);
    }

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
    record Impl(Expression expr) implements UnaryPredicate {
    }
}
