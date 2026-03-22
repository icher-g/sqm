package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.Objects;

/**
 * Table-variable reference used in places such as {@code OUTPUT INTO @audit}.
 * <p>
 * The stored identifier is the canonical variable name without the leading
 * variable sigil used by the dialect.
 */
public non-sealed interface VariableTableRef extends TableRef {

    /**
     * Creates a table-variable reference from a variable name.
     *
     * @param name canonical variable name without leading {@code @}
     * @return table-variable reference
     */
    static VariableTableRef of(String name) {
        return of(Identifier.of(name));
    }

    /**
     * Creates a table-variable reference from a quote-aware identifier.
     *
     * @param name canonical variable name without leading {@code @}
     * @return table-variable reference
     */
    static VariableTableRef of(Identifier name) {
        return new Impl(name);
    }

    /**
     * Returns the canonical table-variable name without the leading dialect sigil.
     *
     * @return table-variable identifier
     */
    Identifier name();

    /**
     * Accepts a visitor.
     *
     * @param v visitor instance
     * @param <R> result type
     * @return visitor result
     */
    @Override
    default <R> R accept(NodeVisitor<R> v) {
        return v.visitVariableTableRef(this);
    }

    /**
     * Default immutable implementation.
     *
     * @param name canonical variable name without leading {@code @}
     */
    record Impl(Identifier name) implements VariableTableRef {

        /**
         * Creates a table-variable reference implementation.
         *
         * @param name canonical variable name without leading {@code @}
         */
        public Impl {
            Objects.requireNonNull(name, "name");
            if (name.quoted()) {
                throw new IllegalArgumentException("Table variable names cannot be quoted");
            }
            if (name.value().isBlank()) {
                throw new IllegalArgumentException("Table variable name cannot be blank");
            }
        }
    }
}
