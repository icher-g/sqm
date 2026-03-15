package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.Objects;

/**
 * SQL Server pseudo-column reference used inside a DML {@code OUTPUT} clause,
 * such as {@code inserted.id} or {@code deleted.status}.
 */
public non-sealed interface OutputColumnExpr extends Expression {

    /**
     * Creates an {@code inserted.<column>} output expression.
     *
     * @param column output column identifier
     * @return output column expression
     */
    static OutputColumnExpr inserted(Identifier column) {
        return of(OutputRowSource.INSERTED, column);
    }

    /**
     * Creates a {@code deleted.<column>} output expression.
     *
     * @param column output column identifier
     * @return output column expression
     */
    static OutputColumnExpr deleted(Identifier column) {
        return of(OutputRowSource.DELETED, column);
    }

    /**
     * Creates an output column expression.
     *
     * @param source pseudo-row source
     * @param column output column identifier
     * @return output column expression
     */
    static OutputColumnExpr of(OutputRowSource source, Identifier column) {
        return new Impl(source, column);
    }

    /**
     * Returns the SQL Server pseudo-row source.
     *
     * @return output row source
     */
    OutputRowSource source();

    /**
     * Returns the referenced output column identifier.
     *
     * @return output column identifier
     */
    Identifier column();

    /**
     * Accepts a visitor.
     *
     * @param v visitor instance
     * @param <R> result type
     * @return visitor result
     */
    @Override
    default <R> R accept(NodeVisitor<R> v) {
        return v.visitOutputColumnExpr(this);
    }

    /**
     * Default immutable implementation.
     *
     * @param source pseudo-row source
     * @param column output column identifier
     */
    record Impl(OutputRowSource source, Identifier column) implements OutputColumnExpr {

        /**
         * Creates an output column expression implementation.
         *
         * @param source pseudo-row source
         * @param column output column identifier
         */
        public Impl {
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(column, "column");
        }
    }
}
