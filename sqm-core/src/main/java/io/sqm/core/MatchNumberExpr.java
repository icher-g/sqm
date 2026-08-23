package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

/**
 * Match ordinal within a pattern-recognition partition.
 */
public non-sealed interface MatchNumberExpr extends Expression {
    /**
     * Returns the singleton match-number expression.
     *
     * @return match-number expression
     */
    static MatchNumberExpr of() {
        return Impl.INSTANCE;
    }

    /**
     * Accepts a node visitor.
     *
     * @param visitor visitor to accept
     * @param <R> visitor result type
     * @return visitor result
     */
    @Override
    default <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitMatchNumberExpr(this);
    }

    /**
     * Immutable match-number implementation.
     */
    record Impl() implements MatchNumberExpr {
        private static final Impl INSTANCE = new Impl();
    }
}
