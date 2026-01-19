package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

/**
 * Represents a SQL pattern-matching predicate such as
 * {@code LIKE}, {@code ILIKE}, or {@code SIMILAR TO}.
 *
 * <p>This predicate compares a value expression against a pattern expression
 * using the operator specified by {@link LikeMode}. Support for individual
 * modes depends on the SQL dialect used during rendering.</p>
 *
 * <p>Negation (e.g. {@code NOT LIKE}, {@code NOT ILIKE}, {@code NOT SIMILAR TO})
 * is modeled explicitly via {@link #negated()}.</p>
 *
 * <p>An optional {@code ESCAPE} expression may be provided to define a custom
 * escape character for the pattern. Dialects may restrict or ignore the
 * {@code ESCAPE} clause depending on the operator and database capabilities.</p>
 *
 * <p>This interface is non-sealed to allow dialect-specific extensions
 * if required.</p>
 *
 * @see LikeMode
 */
public non-sealed interface LikePredicate extends Predicate {

    /**
     * Creates a {@link LikePredicate} without an {@code ESCAPE} clause.
     *
     * @param value   the value expression being matched
     * @param pattern the pattern expression
     * @param negated whether the predicate is negated
     * @return a new {@link LikePredicate} instance
     */
    static LikePredicate of(Expression value, Expression pattern, boolean negated) {
        return new Impl(null, value, pattern, null, negated);
    }

    /**
     * Creates a {@link LikePredicate} without an {@code ESCAPE} clause.
     *
     * @param mode    the pattern matching mode to use
     * @param value   the value expression being matched
     * @param pattern the pattern expression
     * @param negated whether the predicate is negated
     * @return a new {@link LikePredicate} instance
     */
    static LikePredicate of(LikeMode mode, Expression value, Expression pattern, boolean negated) {
        return new Impl(mode, value, pattern, null, negated);
    }

    /**
     * Creates a {@link LikePredicate} with an explicit {@code ESCAPE} clause.
     *
     * @param value   the value expression being matched
     * @param pattern the pattern expression
     * @param escape  the escape character expression
     * @param negated whether the predicate is negated
     * @return a new {@link LikePredicate} instance
     */
    static LikePredicate of(Expression value, Expression pattern, Expression escape, boolean negated) {
        return new Impl(null, value, pattern, escape, negated);
    }

    /**
     * Creates a {@link LikePredicate} with an explicit {@code ESCAPE} clause.
     *
     * @param mode    the pattern matching mode to use
     * @param value   the value expression being matched
     * @param pattern the pattern expression
     * @param escape  the escape character expression
     * @param negated whether the predicate is negated
     * @return a new {@link LikePredicate} instance
     */
    static LikePredicate of(LikeMode mode, Expression value, Expression pattern, Expression escape, boolean negated) {
        return new Impl(mode, value, pattern, escape, negated);
    }

    /**
     * Returns the pattern matching mode used by this predicate.
     *
     * @return the {@link LikeMode}
     */
    LikeMode mode();

    /**
     * Returns the value expression being matched against the pattern.
     *
     * @return the value expression
     */
    Expression value();

    /**
     * Returns the pattern expression.
     *
     * @return the pattern expression
     */
    Expression pattern();

    /**
     * Returns the escape character expression, if present.
     *
     * @return the escape expression, or {@code null} if not specified
     */
    Expression escape();

    /**
     * Indicates whether this predicate is negated.
     *
     * @return {@code true} if the predicate is negated, {@code false} otherwise
     */
    boolean negated();

    /**
     * Returns a copy of this predicate with the given escape character literal.
     *
     * @param escape the escape character as a literal string
     * @return a new {@link LikePredicate} instance with the escape clause applied
     */
    default LikePredicate escape(String escape) {
        return new Impl(mode(), value(), pattern(), Expression.literal(escape), negated());
    }

    /**
     * Returns a copy of this predicate with the given escape expression.
     *
     * @param escape the escape character expression
     * @return a new {@link LikePredicate} instance with the escape clause applied
     */
    default LikePredicate escape(Expression escape) {
        return new Impl(mode(), value(), pattern(), escape, negated());
    }

    /**
     * Accepts a {@link NodeVisitor} and dispatches control to the
     * visitor method corresponding to this predicate type.
     *
     * @param v   the visitor instance to accept (must not be {@code null})
     * @param <R> the result type returned by the visitor
     * @return the result produced by the visitor
     */
    @Override
    default <R> R accept(NodeVisitor<R> v) {
        return v.visitLikePredicate(this);
    }

    /**
     * Default immutable implementation of {@link LikePredicate}.
     *
     * @param mode    the pattern matching mode to use
     * @param value   the value expression being matched
     * @param pattern the pattern expression
     * @param escape  the escape character expression
     * @param negated whether the predicate is negated
     */
    record Impl(
        LikeMode mode,
        Expression value,
        Expression pattern,
        Expression escape,
        boolean negated
    ) implements LikePredicate {
        public Impl {
            mode = (mode == null) ? LikeMode.LIKE : mode;
        }
    }
}

