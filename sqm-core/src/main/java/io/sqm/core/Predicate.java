package io.sqm.core;

/**
 * Boolean-valued expression: usable in WHERE, HAVING, and JOIN ... ON.
 */
public sealed interface Predicate extends Expression
    permits AnyAllPredicate, BetweenPredicate, ComparisonPredicate, CompositePredicate, ExistsPredicate, InPredicate, IsNullPredicate, LikePredicate, NotPredicate, UnaryPredicate {

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
     * Casts current expression to {@link AnyAllPredicate}.
     *
     * @return {@link AnyAllPredicate}.
     */
    default AnyAllPredicate asAnyAll() {
        return this instanceof AnyAllPredicate p ? p : null;
    }

    /**
     * Casts current expression to {@link BetweenPredicate}.
     *
     * @return {@link BetweenPredicate}.
     */
    default BetweenPredicate asBetween() {
        return this instanceof BetweenPredicate p ? p : null;
    }

    /**
     * Casts current expression to {@link ComparisonPredicate}.
     *
     * @return {@link ComparisonPredicate}.
     */
    default ComparisonPredicate asComparison() {
        return this instanceof ComparisonPredicate p ? p : null;
    }

    /**
     * Casts current expression to {@link CompositePredicate}.
     *
     * @return {@link CompositePredicate}.
     */
    default CompositePredicate asComposite() {
        return this instanceof CompositePredicate p ? p : null;
    }

    /**
     * Casts current expression to {@link ExistsPredicate}.
     *
     * @return {@link ExistsPredicate}.
     */
    default ExistsPredicate asExists() {
        return this instanceof ExistsPredicate p ? p : null;
    }

    /**
     * Casts current expression to {@link InPredicate}.
     *
     * @return {@link InPredicate}.
     */
    default InPredicate asIn() {
        return this instanceof InPredicate p ? p : null;
    }

    /**
     * Casts current expression to {@link IsNullPredicate}.
     *
     * @return {@link IsNullPredicate}.
     */
    default IsNullPredicate asIsNull() {
        return this instanceof IsNullPredicate p ? p : null;
    }

    /**
     * Casts current expression to {@link LikePredicate}.
     *
     * @return {@link LikePredicate}.
     */
    default LikePredicate asLike() {
        return this instanceof LikePredicate p ? p : null;
    }

    /**
     * Casts current expression to {@link NotPredicate}.
     *
     * @return {@link NotPredicate}.
     */
    default NotPredicate asNot() {
        return this instanceof NotPredicate p ? p : null;
    }
}
