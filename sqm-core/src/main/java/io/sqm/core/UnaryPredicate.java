package io.sqm.core;

import io.sqm.core.internal.UnaryPredicateImpl;

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
        return new UnaryPredicateImpl(expr);
    }

    Expression expr();
}
