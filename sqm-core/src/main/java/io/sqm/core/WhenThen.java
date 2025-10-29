package io.sqm.core;

import io.sqm.core.internal.WhenThenImpl;

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
        return new WhenThenImpl(when, then);
    }

    /**
     * Creates a WHEN...THEN statement.
     *
     * @param when a WHEN predicate.
     * @return A newly created instance of the WHEN...THEN statement.
     */
    static WhenThen of(Predicate when) {
        return new WhenThenImpl(when, null);
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
        return new WhenThenImpl(when(), then);
    }

    /**
     * Adds a THEN statement.
     *
     * @param then a THEN statement.
     * @return this.
     */
    default WhenThen then(Object then) {
        return new WhenThenImpl(when(), Expression.literal(then));
    }
}
