package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.Objects;

/**
 * Resume position used after a row-pattern match is accepted.
 */
public non-sealed interface AfterMatchSkip extends Node {
    /**
     * General skip strategy.
     */
    enum Kind {
        /** Resumes after the last row of the accepted match. */
        PAST_LAST_ROW,
        /** Resumes at the row after the match start. */
        TO_NEXT_ROW,
        /** Resumes at a named pattern variable. */
        TO_VARIABLE
    }

    /**
     * Position selected when skipping to a variable.
     */
    enum Position {
        /** Uses the variable's default position. */
        DEFAULT,
        /** Selects the first occurrence of the variable. */
        FIRST,
        /** Selects the last occurrence of the variable. */
        LAST
    }

    /**
     * Creates an after-match skip specification.
     *
     * @param kind skip strategy
     * @param position variable position
     * @param variable variable target, required only for {@link Kind#TO_VARIABLE}
     * @return immutable after-match skip specification
     */
    static AfterMatchSkip of(Kind kind, Position position, Identifier variable) {
        return new Impl(kind, position, variable);
    }

    /**
     * Returns the skip strategy.
     *
     * @return skip strategy
     */
    Kind kind();

    /**
     * Returns the variable position.
     *
     * @return variable position
     */
    Position position();

    /**
     * Returns the variable target.
     *
     * @return variable target, or {@code null} for non-variable strategies
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
        return visitor.visitAfterMatchSkip(this);
    }

    /**
     * Immutable after-match skip implementation.
     *
     * @param kind skip strategy
     * @param position variable position
     * @param variable variable target
     */
    record Impl(Kind kind, Position position, Identifier variable) implements AfterMatchSkip {
        /**
         * Validates the after-match skip shape.
         *
         * @param kind skip strategy
         * @param position variable position
         * @param variable variable target
         */
        public Impl {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(position, "position");
            if (kind == Kind.TO_VARIABLE) {
                Objects.requireNonNull(variable, "variable");
            }
            else if (position != Position.DEFAULT || variable != null) {
                throw new IllegalArgumentException("Only TO_VARIABLE accepts a position or variable");
            }
        }
    }
}
