package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.List;
import java.util.Objects;

/**
 * SQL Server DML {@code OUTPUT} clause with optional {@code INTO} target.
 */
public non-sealed interface OutputClause extends Node {

    /**
     * Creates an output clause without an {@code INTO} target.
     *
     * @param items projected output items
     * @return output clause
     */
    static OutputClause of(List<OutputItem> items) {
        return of(items, null);
    }

    /**
     * Creates an output clause.
     *
     * @param items projected output items
     * @param into optional output-into target
     * @return output clause
     */
    static OutputClause of(List<OutputItem> items, OutputInto into) {
        return new Impl(items, into);
    }

    /**
     * Returns the projected output items.
     *
     * @return immutable output item list
     */
    List<OutputItem> items();

    /**
     * Returns the optional output-into target.
     *
     * @return output-into target or {@code null}
     */
    OutputInto into();

    /**
     * Accepts a visitor.
     *
     * @param v visitor instance
     * @param <R> result type
     * @return visitor result
     */
    @Override
    default <R> R accept(NodeVisitor<R> v) {
        return v.visitOutputClause(this);
    }

    /**
     * Default immutable implementation.
     *
     * @param items projected output items
     * @param into optional output-into target
     */
    record Impl(List<OutputItem> items, OutputInto into) implements OutputClause {

        /**
         * Creates an output clause implementation.
         *
         * @param items projected output items
         * @param into optional output-into target
         */
        public Impl {
            items = List.copyOf(Objects.requireNonNull(items, "items"));
            if (items.isEmpty()) {
                throw new IllegalArgumentException("items must not be empty");
            }
        }
    }
}
