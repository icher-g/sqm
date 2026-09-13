package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;
import java.util.List;
import java.util.Objects;

/**
 * An aggregate over a selected set of model cells.
 *
 * <p>This node represents {@code SUM(sales)['Bike', sales_year BETWEEN 2023 AND 2025]}, including the
 * aggregate invocation and its bracketed selectors.</p>
 * <pre>{@code
 * SELECT product, sales_year, sales
 * FROM sales_data
 * MODEL
 *   DIMENSION BY (product, sales_year)
 *   MEASURES (amount AS sales)
 *   RULES (
 *     sales['Bike', 2026] = SUM(sales)['Bike', sales_year BETWEEN 2023 AND 2025]
 *   )
 * }</pre>
 */
public non-sealed interface ModelAggregateExpr extends Expression {
    /**
     * Creates an immutable ModelAggregateExpr with its complete state.
     *
     * @param aggregate aggregate invocation
     * @param selectors non-empty cell selectors
     * @return immutable node
     */
    static ModelAggregateExpr of(FunctionExpr aggregate, List<CellSelector> selectors) {
        return new Impl(aggregate, selectors);
    }

    /**
     * Returns the aggregate invocation.
     *
     * @return aggregate invocation
     */
    FunctionExpr aggregate();

    /**
     * Returns the non-empty cell selectors.
     *
     * @return non-empty cell selectors
     */
    List<CellSelector> selectors();

    /** {@inheritDoc} */
    @Override
    default <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitModelAggregateExpr(this);
    }

    /**
     * Immutable implementation.
     *
     * @param aggregate aggregate invocation
     * @param selectors non-empty cell selectors
     */
    record Impl(FunctionExpr aggregate, List<CellSelector> selectors) implements ModelAggregateExpr {
        /**
         * Validates and copies the supplied state.
         *
         * @param aggregate aggregate invocation
         * @param selectors non-empty cell selectors
         */
        public Impl {
            Objects.requireNonNull(aggregate, "aggregate");
            selectors = List.copyOf(selectors);
            if (selectors.isEmpty()) {
                throw new IllegalArgumentException("selectors must not be empty");
            }
        }
    }
}
