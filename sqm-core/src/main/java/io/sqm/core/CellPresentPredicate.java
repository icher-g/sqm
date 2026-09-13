package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;
import java.util.Objects;

/**
 * A test for whether a cell existed in the model's input.
 *
 * <p>This node represents {@code sales['Bike', 2025] IS PRESENT}, the condition inside the {@code CASE}
 * expression.</p>
 * <pre>{@code
 * SELECT product, sales_year, sales
 * FROM sales_data
 * MODEL
 *   DIMENSION BY (product, sales_year)
 *   MEASURES (amount AS sales)
 *   RULES (
 *     sales['Bike', 2026] = CASE WHEN sales['Bike', 2025] IS PRESENT
 *       THEN 100 ELSE 0 END
 *   )
 * }</pre>
 */
public non-sealed interface CellPresentPredicate extends Predicate {
    /**
     * Creates an immutable CellPresentPredicate with its complete state.
     *
     * @param cell cell whose input presence is tested
     * @return immutable node
     */
    static CellPresentPredicate of(CellRefExpr cell) {
        return new Impl(cell);
    }

    /**
     * Returns the cell whose input presence is tested.
     *
     * @return cell whose input presence is tested
     */
    CellRefExpr cell();

    /** {@inheritDoc} */
    @Override
    default <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitCellPresentPredicate(this);
    }

    /**
     * Immutable implementation.
     *
     * @param cell cell whose input presence is tested
     */
    record Impl(CellRefExpr cell) implements CellPresentPredicate {
        /**
         * Validates and copies the supplied state.
         *
         * @param cell cell whose input presence is tested
         */
        public Impl {
            Objects.requireNonNull(cell, "cell");
        }
    }
}
