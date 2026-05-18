package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.Objects;

/**
 * Ordinality column inside a {@code JSON_TABLE} column list.
 */
public non-sealed interface JsonTableOrdinalityColumn extends JsonTableColumn {
    /**
     * Creates an ordinality JSON table column.
     *
     * @param name column name
     * @return ordinality JSON table column
     */
    static JsonTableOrdinalityColumn of(Identifier name) {
        return new Impl(name);
    }

    /**
     * Returns the column name.
     *
     * @return column name
     */
    Identifier name();

    /**
     * Accepts a node visitor.
     *
     * @param v   visitor
     * @param <R> visitor result type
     * @return visitor result
     */
    @Override
    default <R> R accept(NodeVisitor<R> v) {
        return v.visitJsonTableOrdinalityColumn(this);
    }

    /**
     * Immutable JSON_TABLE ordinality column implementation.
     *
     * @param name column name
     */
    record Impl(Identifier name) implements JsonTableOrdinalityColumn {
        /**
         * Creates an immutable JSON_TABLE ordinality column.
         */
        public Impl {
            Objects.requireNonNull(name, "name");
        }
    }
}