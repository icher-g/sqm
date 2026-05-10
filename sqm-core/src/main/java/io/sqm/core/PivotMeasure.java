package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.Objects;

/**
 * Aggregate measure produced by a relational {@link PivotTable}.
 */
public non-sealed interface PivotMeasure extends Node {
    /**
     * Creates a pivot measure.
     *
     * @param aggregateFunction aggregate function used by the pivot
     * @param alias optional measure alias
     * @return pivot measure
     */
    static PivotMeasure of(FunctionExpr aggregateFunction, Identifier alias) {
        return new Impl(aggregateFunction, alias);
    }

    /**
     * Returns the aggregate function used by the pivot.
     *
     * @return aggregate function
     */
    FunctionExpr aggregateFunction();

    /**
     * Returns the optional measure alias.
     *
     * @return alias or {@code null}
     */
    Identifier alias();

    /**
     * Creates a copy with the provided alias.
     *
     * @param alias measure alias
     * @return pivot measure with alias
     */
    default PivotMeasure as(String alias) {
        return as(alias == null ? null : Identifier.of(alias));
    }

    /**
     * Creates a copy with the provided alias.
     *
     * @param alias measure alias
     * @return pivot measure with alias
     */
    default PivotMeasure as(Identifier alias) {
        return of(aggregateFunction(), alias);
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
        return v.visitPivotMeasure(this);
    }

    /**
     * Immutable pivot measure implementation.
     *
     * @param aggregateFunction aggregate function used by the pivot
     * @param alias optional measure alias
     */
    record Impl(FunctionExpr aggregateFunction, Identifier alias) implements PivotMeasure {
        /**
         * Creates an immutable pivot measure.
         */
        public Impl {
            Objects.requireNonNull(aggregateFunction, "aggregateFunction");
        }
    }
}
