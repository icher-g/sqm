package io.sqm.core;

import io.sqm.core.match.Match;
import io.sqm.core.match.PredicateMatch;

/**
 * Boolean-valued expression: usable in WHERE, HAVING, and JOIN ... ON.
 */
public sealed interface Predicate extends Expression
    permits AnyAllPredicate, BetweenPredicate, ComparisonPredicate, CompositePredicate, DialectPredicate, ExistsPredicate, InPredicate, IsNullPredicate, LikePredicate, NotPredicate, UnaryPredicate {

    /**
     * Creates a negate predicate for the provided predicate.
     *
     * @param predicate a predicate to negate.
     * @return A newly created instance of a predicate.
     */
    static NotPredicate not(Predicate predicate) {
        return NotPredicate.of(predicate);
    }

    /**
     * Creates a unary predicate.
     *
     * @param expr a boolean expression: TRUE, FALSE or a boolean column.
     * @return a new instance of the unary predicate.
     */
    static UnaryPredicate unary(Expression expr) {
        return UnaryPredicate.of(expr);
    }

    /**
     * Creates EXISTS predicate.
     * <p>For example:</p>
     * <pre>
     *     {@code
     *     SELECT *
     *     FROM customers c
     *     WHERE EXISTS (
     *         SELECT 1
     *         FROM orders o
     *         WHERE o.customer_id = c.id
     *     );
     *     }
     * </pre>
     *
     * @param subquery a sub query which resul to check.
     * @return A newly created EXISTS predicate.
     */
    static ExistsPredicate exists(Query subquery) {
        return ExistsPredicate.of(subquery, false);
    }

    /**
     * Creates NOT EXISTS predicate.
     * <p>For example:</p>
     * <pre>
     *     {@code
     *     SELECT *
     *     FROM customers c
     *     WHERE NOT EXISTS (
     *         SELECT 1
     *         FROM orders o
     *         WHERE o.customer_id = c.id
     *     );
     *     }
     * </pre>
     *
     * @param subquery a sub query which resul to check.
     * @return A newly created NOT EXISTS predicate.
     */
    static ExistsPredicate notExists(Query subquery) {
        return ExistsPredicate.of(subquery, true);
    }

    /**
     * Creates a composite predicate with AND operation between the current predicate and the provided one.
     *
     * @param other a predicate to compose.
     * @return A newly created instance of a filter.
     */
    default AndPredicate and(Predicate other) {
        return AndPredicate.of(this, other);
    }

    /**
     * Creates a composite predicate with OR operation between the current predicate and the provided one.
     *
     * @param other a predicate to compose.
     * @return A newly created instance of a filter.
     */
    default OrPredicate or(Predicate other) {
        return OrPredicate.of(this, other);
    }

    /**
     * Creates a negate predicate for the current predicate.
     *
     * @return A newly created instance of a predicate.
     */
    default NotPredicate not() {
        return NotPredicate.of(this);
    }

    /**
     * Creates a new matcher for the current {@link Predicate}.
     *
     * @param <R> the result type
     * @return a new {@code PredicateMatch}.
     */
    default <R> PredicateMatch<R> matchPredicate() {
        return Match.predicate(this);
    }
}
