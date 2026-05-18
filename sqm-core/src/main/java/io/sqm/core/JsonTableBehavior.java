package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.Objects;

/**
 * Behavior applied by {@code JSON_TABLE} when a column path is empty or errors.
 */
public non-sealed interface JsonTableBehavior extends Node {
    /**
     * Creates a behavior with no default expression.
     *
     * @param kind behavior kind
     * @return JSON table behavior
     */
    static JsonTableBehavior of(Kind kind) {
        return of(kind, null);
    }

    /**
     * Creates a behavior.
     *
     * @param kind behavior kind
     * @param defaultExpression expression used by {@link Kind#DEFAULT}
     * @return JSON table behavior
     */
    static JsonTableBehavior of(Kind kind, Expression defaultExpression) {
        return new Impl(kind, defaultExpression);
    }

    /**
     * Returns behavior kind.
     *
     * @return behavior kind
     */
    Kind kind();

    /**
     * Returns the default expression, when {@link #kind()} is {@link Kind#DEFAULT}.
     *
     * @return default expression, or {@code null}
     */
    Expression defaultExpression();

    /**
     * Accepts a node visitor.
     *
     * @param v visitor
     * @param <R> visitor result type
     * @return visitor result
     */
    @Override
    default <R> R accept(NodeVisitor<R> v) {
        return v.visitJsonTableBehavior(this);
    }

    /**
     * JSON table behavior kind.
     */
    enum Kind {
        /**
         * Raise an error.
         */
        ERROR,
        /**
         * Produce SQL {@code NULL}.
         */
        NULL,
        /**
         * Produce an empty JSON array or dialect default empty value.
         */
        EMPTY,
        /**
         * Produce the provided default expression.
         */
        DEFAULT
    }

    /**
     * Immutable JSON table behavior implementation.
     *
     * @param kind behavior kind
     * @param defaultExpression default expression, or {@code null}
     */
    record Impl(Kind kind, Expression defaultExpression) implements JsonTableBehavior {
        /**
         * Creates an immutable JSON table behavior.
         */
        public Impl {
            Objects.requireNonNull(kind, "kind");
            if (kind == Kind.DEFAULT && defaultExpression == null) {
                throw new IllegalArgumentException("DEFAULT JSON_TABLE behavior requires a default expression");
            }
            if (kind != Kind.DEFAULT && defaultExpression != null) {
                throw new IllegalArgumentException("Only DEFAULT JSON_TABLE behavior may define a default expression");
            }
        }
    }
}
