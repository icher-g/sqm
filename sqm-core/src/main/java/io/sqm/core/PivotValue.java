package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.Objects;

/**
 * Explicit value listed in a relational {@link PivotTable}.
 */
public non-sealed interface PivotValue extends Node {
    /**
     * Creates a pivot value.
     *
     * @param value source value that maps to a pivoted output column
     * @param alias optional output alias
     * @return pivot value
     */
    static PivotValue of(Expression value, Identifier alias) {
        return new Impl(value, alias);
    }

    /**
     * Returns the source value that maps to a pivoted output column.
     *
     * @return pivot value expression
     */
    Expression value();

    /**
     * Returns the optional output alias.
     *
     * @return alias or {@code null}
     */
    Identifier alias();

    /**
     * Creates a copy with the provided alias.
     *
     * @param alias output alias
     * @return pivot value with alias
     */
    default PivotValue as(String alias) {
        return as(alias == null ? null : Identifier.of(alias));
    }

    /**
     * Creates a copy with the provided alias.
     *
     * @param alias output alias
     * @return pivot value with alias
     */
    default PivotValue as(Identifier alias) {
        return of(value(), alias);
    }

    /**
     * Accepts a node visitor.
     *
     * @param v visitor
     * @param <R> visitor result type
     * @return visitor result
     */
    @Override
    default <R> R accept(NodeVisitor<R> v) {
        return v.visitPivotValue(this);
    }

    /**
     * Immutable pivot value implementation.
     *
     * @param value source value that maps to a pivoted output column
     * @param alias optional output alias
     */
    record Impl(Expression value, Identifier alias) implements PivotValue {
        /**
         * Creates an immutable pivot value.
         */
        public Impl {
            Objects.requireNonNull(value, "value");
        }
    }
}
