package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.Objects;

/**
 * Represents a value expression that reads from a database sequence.
 */
public non-sealed interface SequenceValueExpr extends Expression {

    /**
     * Creates a sequence value expression.
     *
     * @param sequence sequence name
     * @param kind value operation to read from the sequence
     * @return a sequence value expression
     */
    static SequenceValueExpr of(QualifiedName sequence, SequenceValueKind kind) {
        return new Impl(sequence, kind);
    }

    /**
     * Returns the referenced sequence name.
     *
     * @return sequence name
     */
    QualifiedName sequence();

    /**
     * Returns which sequence value is requested.
     *
     * @return sequence value kind
     */
    SequenceValueKind kind();

    /**
     * Accepts a visitor.
     *
     * @param v visitor instance
     * @param <R> visitor result type
     * @return visitor result
     */
    @Override
    default <R> R accept(NodeVisitor<R> v) {
        return v.visitSequenceValueExpr(this);
    }

    /**
     * Default immutable implementation.
     *
     * @param sequence sequence name
     * @param kind value operation to read from the sequence
     */
    record Impl(QualifiedName sequence, SequenceValueKind kind) implements SequenceValueExpr {
        /**
         * Creates a sequence value expression implementation.
         *
         * @param sequence sequence name
         * @param kind value operation to read from the sequence
         */
        public Impl {
            Objects.requireNonNull(sequence, "sequence");
            Objects.requireNonNull(kind, "kind");
        }
    }
}
