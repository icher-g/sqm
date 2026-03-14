package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.Objects;

/**
 * Represents a single {@code column = value} assignment in an {@code UPDATE} statement.
 */
public non-sealed interface Assignment extends Node {

    /**
     * Creates an immutable assignment.
     *
     * @param column target column qualified name
     * @param value assigned expression
     * @return immutable assignment
     */
    static Assignment of(QualifiedName column, Expression value) {
        return new Impl(column, value);
    }

    /**
     * Creates an immutable assignment.
     *
     * @param column target column identifier
     * @param value assigned expression
     * @return immutable assignment
     */
    static Assignment of(Identifier column, Expression value) {
        return of(QualifiedName.of(column), value);
    }

    /**
     * Returns the target column qualified name.
     *
     * @return target column
     */
    QualifiedName column();

    /**
     * Returns the assigned expression.
     *
     * @return assigned expression
     */
    Expression value();

    /**
     * Accepts a {@link NodeVisitor}.
     *
     * @param visitor visitor instance
     * @param <R> result type
     * @return visitor result
     */
    @Override
    default <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitAssignment(this);
    }

    /**
     * Default immutable assignment implementation.
     *
     * @param column target column qualified name
     * @param value assigned expression
     */
    record Impl(QualifiedName column, Expression value) implements Assignment {
        /**
         * Creates an immutable assignment implementation.
         */
        public Impl {
            Objects.requireNonNull(column, "column");
            Objects.requireNonNull(value, "value");
        }
    }
}
