package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.List;
import java.util.Objects;

/**
 * SQL Server {@code OUTPUT ... INTO ...} target specification.
 */
public non-sealed interface OutputInto extends Node {

    /**
     * Creates an output-into target without an explicit target column list.
     *
     * @param target target table
     * @return output-into specification
     */
    static OutputInto of(Table target) {
        return of(target, List.of());
    }

    /**
     * Creates an output-into target specification.
     *
     * @param target target table
     * @param columns optional target columns
     * @return output-into specification
     */
    static OutputInto of(Table target, List<Identifier> columns) {
        return new Impl(target, columns);
    }

    /**
     * Returns the target table that receives the emitted rows.
     *
     * @return output target table
     */
    Table target();

    /**
     * Returns the optional target column list.
     *
     * @return immutable target column list
     */
    List<Identifier> columns();

    /**
     * Accepts a visitor.
     *
     * @param v visitor instance
     * @param <R> result type
     * @return visitor result
     */
    @Override
    default <R> R accept(NodeVisitor<R> v) {
        return v.visitOutputInto(this);
    }

    /**
     * Default immutable implementation.
     *
     * @param target target table
     * @param columns optional target columns
     */
    record Impl(Table target, List<Identifier> columns) implements OutputInto {

        /**
         * Creates an output-into implementation.
         *
         * @param target target table
         * @param columns optional target columns
         */
        public Impl {
            Objects.requireNonNull(target, "target");
            columns = columns == null ? List.of() : List.copyOf(columns);
        }
    }
}
