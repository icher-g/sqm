package io.sqm.core;

import io.sqm.core.match.AssignmentMatch;
import io.sqm.core.walk.NodeVisitor;

import java.util.Objects;

/**
 * Represents a single {@code column = value} assignment in an {@code UPDATE} statement.
 */
public non-sealed interface Assignment extends Node {

    /**
     * Creates an immutable assignment.
     *
     * @param column target column identifier
     * @param value assigned expression
     * @return immutable assignment
     */
    static Assignment of(Identifier column, Expression value) {
        return new Impl(column, value);
    }

    /**
     * Returns the target column identifier.
     *
     * @return target column
     */
    Identifier column();

    /**
     * Returns the assigned expression.
     *
     * @return assigned expression
     */
    Expression value();

    /**
     * Creates a matcher for the current assignment.
     *
     * @param <R> result type
     * @return assignment matcher
     */
    default <R> AssignmentMatch<R> matchAssignment() {
        return AssignmentMatch.match(this);
    }

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
     * @param column target column identifier
     * @param value assigned expression
     */
    record Impl(Identifier column, Expression value) implements Assignment {
        /**
         * Creates an immutable assignment implementation.
         */
        public Impl {
            Objects.requireNonNull(column, "column");
            Objects.requireNonNull(value, "value");
        }
    }
}
