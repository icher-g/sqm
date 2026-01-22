package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

/**
 * Represents WHEN...THEN statement used in CASE expression.
 * <p>For example:</p>
 * <pre>
 *     {@code
 *     CASE WHEN x = 1 THEN 10 WHEN x = 2 THEN 20 END AS result
 *     }
 * </pre>
 */
public non-sealed interface WhenThen extends Node {
    /**
     * Creates a WHEN...THEN statement.
     *
     * @param when a WHEN predicate.
     * @param then a THEN expression.
     * @return A newly created instance of the WHEN...THEN statement.
     */
    static WhenThen of(Predicate when, Expression then) {
        return new Impl(when, then);
    }

    /**
     * Creates a WHEN statement.
     *
     * @param when a WHEN predicate.
     * @return A newly created instance of the WHEN...THEN statement with only WHEN part.
     */
    static WhenThen when(Predicate when) {
        return new Impl(when, null);
    }

    /**
     * Gets a WHEN predicate.
     *
     * @return a WHEN predicate.
     */
    Predicate when();

    /**
     * Gets a THEN expression.
     *
     * @return a THEN expression.
     */
    Expression then();

    /**
     * Adds a THEN statement.
     *
     * @param then a THEN statement.
     * @return this.
     */
    default WhenThen then(Expression then) {
        return new Impl(when(), then);
    }

    /**
     * Adds a THEN statement.
     *
     * @param then a THEN statement.
     * @return this.
     */
    default WhenThen then(Object then) {
        return new Impl(when(), Expression.literal(then));
    }

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
        return v.visitWhenThen(this);
    }

    /**
     * Represents WHEN...THEN statement used in CASE expression.
     * <p>For example:</p>
     * <pre>
     *     {@code
     *     CASE WHEN x = 1 THEN 10 WHEN x = 2 THEN 20 END AS result
     *     }
     * </pre>
     *
     * @param when a WHEN predicate.
     * @param then a THEN expression.
     */
    record Impl(Predicate when, Expression then) implements WhenThen {
    }
}
