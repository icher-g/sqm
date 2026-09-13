package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

/**
 * The current target cell's dimension value.
 *
 * <p>This node represents {@code CV(product)} below. An absent dimension represents {@code CV()}, whose
 * dimension is inferred from its position in a cell selector.</p>
 * <pre>{@code
 * SELECT product, sales_year, sales
 * FROM sales_data
 * MODEL
 *   DIMENSION BY (product, sales_year)
 *   MEASURES (amount AS sales)
 *   RULES (
 *     sales[ANY, 2026] = sales[CV(product), 2025] * 1.1
 *   )
 * }</pre>
 */
public non-sealed interface CurrentDimensionExpr extends Expression {
    /**
     * Creates an immutable CurrentDimensionExpr with its complete state.
     *
     * @param dimension optional explicit dimension name; {@code null} when absent
     * @return immutable node
     */
    static CurrentDimensionExpr of(Identifier dimension) {
        return new Impl(dimension);
    }

    /**
     * Returns the optional explicit dimension name.
     *
     * @return optional explicit dimension name; {@code null} when absent
     */
    Identifier dimension();

    /** {@inheritDoc} */
    @Override
    default <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitCurrentDimensionExpr(this);
    }

    /**
     * Immutable implementation.
     *
     * @param dimension optional explicit dimension name; {@code null} when absent
     */
    record Impl(Identifier dimension) implements CurrentDimensionExpr {
    }
}
