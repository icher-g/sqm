package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

/**
 * Represents a regular expression pattern matching predicate.
 *
 * <p>This predicate compares a value expression against a regular expression
 * pattern expression using a dialect-specific regular expression matching
 * operator.</p>
 *
 * <p>The regular expression pattern is treated as an opaque expression and is
 * never modified, interpreted, or augmented by SQM.</p>
 *
 * <p>Case sensitivity and negation are modeled explicitly via
 * {@link RegexMode} and {@link #negated()}.</p>
 *
 * <p>This interface is non-sealed to allow dialect-specific extensions
 * if required.</p>
 *
 * @see RegexMode
 */
public non-sealed interface RegexPredicate extends Predicate {

    /**
     * Creates a case-sensitive regular expression match predicate
     * using the default {@link RegexMode#MATCH} mode.
     *
     * @param value   the value expression being matched
     * @param pattern the regular expression pattern expression
     * @param negated whether the predicate is negated
     * @return a new {@link RegexPredicate} instance
     */
    static RegexPredicate of(Expression value, Expression pattern, boolean negated) {
        return new Impl(RegexMode.MATCH, value, pattern, negated);
    }

    /**
     * Creates a regular expression match predicate using the specified mode.
     *
     * @param mode    the regular expression matching mode to use
     * @param value   the value expression being matched
     * @param pattern the regular expression pattern expression
     * @param negated whether the predicate is negated
     * @return a new {@link RegexPredicate} instance
     */
    static RegexPredicate of(RegexMode mode, Expression value, Expression pattern, boolean negated) {
        return new Impl(mode, value, pattern, negated);
    }

    /**
     * Returns the regular expression matching mode used by this predicate.
     *
     * @return the {@link RegexMode}
     */
    RegexMode mode();

    /**
     * Returns the value expression being matched against the regular expression.
     *
     * @return the value expression
     */
    Expression value();

    /**
     * Returns the regular expression pattern expression.
     *
     * <p>The pattern expression is rendered verbatim and is not modified
     * or analyzed by SQM.</p>
     *
     * @return the pattern expression
     */
    Expression pattern();

    /**
     * Indicates whether this predicate is negated.
     *
     * @return {@code true} if the predicate is negated, {@code false} otherwise
     */
    boolean negated();

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
        return v.visitRegexPredicate(this);
    }

    /**
     * Default immutable implementation of {@link RegexPredicate}.
     */
    record Impl(
        RegexMode mode,
        Expression value,
        Expression pattern,
        boolean negated
    ) implements RegexPredicate {
    }
}



