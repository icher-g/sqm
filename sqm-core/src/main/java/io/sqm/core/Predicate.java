package io.sqm.core;

import java.util.Optional;

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
     * Casts current expression to {@link AnyAllPredicate} if possible.
     *
     * @return {@link Optional}<{@link AnyAllPredicate}>.
     */
    default Optional<AnyAllPredicate> asAnyAll() {
        return this instanceof AnyAllPredicate p ? Optional.of(p) : Optional.empty();
    }

    /**
     * Casts current expression to {@link BetweenPredicate} if possible.
     *
     * @return {@link Optional}<{@link BetweenPredicate}>.
     */
    default Optional<BetweenPredicate> asBetween() {
        return this instanceof BetweenPredicate p ? Optional.of(p) : Optional.empty();
    }

    /**
     * Casts current expression to {@link ComparisonPredicate} if possible.
     *
     * @return {@link Optional}<{@link ComparisonPredicate}>.
     */
    default Optional<ComparisonPredicate> asComparison() {
        return this instanceof ComparisonPredicate p ? Optional.of(p) : Optional.empty();
    }

    /**
     * Casts current expression to {@link CompositePredicate} if possible.
     *
     * @return {@link Optional}<{@link CompositePredicate}>.
     */
    default Optional<CompositePredicate> asComposite() {
        return this instanceof CompositePredicate p ? Optional.of(p) : Optional.empty();
    }

    /**
     * Casts current expression to {@link ExistsPredicate} if possible.
     *
     * @return {@link Optional}<{@link ExistsPredicate}>.
     */
    default Optional<ExistsPredicate> asExists() {
        return this instanceof ExistsPredicate p ? Optional.of(p) : Optional.empty();
    }

    /**
     * Casts current expression to {@link InPredicate} if possible.
     *
     * @return {@link Optional}<{@link InPredicate}>.
     */
    default Optional<InPredicate> asIn() {
        return this instanceof InPredicate p ? Optional.of(p) : Optional.empty();
    }

    /**
     * Casts current expression to {@link IsNullPredicate} if possible.
     *
     * @return {@link Optional}<{@link IsNullPredicate}>.
     */
    default Optional<IsNullPredicate> asIsNull() {
        return this instanceof IsNullPredicate p ? Optional.of(p) : Optional.empty();
    }

    /**
     * Casts current expression to {@link LikePredicate} if possible.
     *
     * @return {@link Optional}<{@link LikePredicate}>.
     */
    default Optional<LikePredicate> asLike() {
        return this instanceof LikePredicate p ? Optional.of(p) : Optional.empty();
    }

    /**
     * Casts current expression to {@link NotPredicate} if possible.
     *
     * @return {@link Optional}<{@link NotPredicate}>.
     */
    default Optional<NotPredicate> asNot() {
        return this instanceof NotPredicate p ? Optional.of(p) : Optional.empty();
    }
}
