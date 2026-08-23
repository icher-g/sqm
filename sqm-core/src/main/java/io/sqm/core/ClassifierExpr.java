package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

/**
 * Row-pattern classifier expression.
 */
public non-sealed interface ClassifierExpr extends Expression {
    /**
     * Creates a classifier expression.
     *
     * @param variable optional primary or union pattern variable
     * @return immutable classifier expression
     */
    static ClassifierExpr of(Identifier variable) {
        return new Impl(variable);
    }

    /**
     * Returns the optional classifier variable.
     *
     * @return pattern variable, or {@code null} for the current-row classifier
     */
    Identifier variable();

    /**
     * Accepts a node visitor.
     *
     * @param visitor visitor to accept
     * @param <R> visitor result type
     * @return visitor result
     */
    @Override
    default <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitClassifierExpr(this);
    }

    /**
     * Immutable classifier implementation.
     *
     * @param variable optional primary or union pattern variable
     */
    record Impl(Identifier variable) implements ClassifierExpr {
    }
}
