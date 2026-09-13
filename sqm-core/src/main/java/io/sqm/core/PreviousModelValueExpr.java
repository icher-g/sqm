package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;
import java.util.Objects;

/**
 * A cell value from the preceding model iteration.
 *
 * <p>This node represents {@code PREVIOUS(balance[1])}. Oracle permits this expression in the iteration's
 * {@code UNTIL} condition, not in an ordinary rule assignment.</p>
 * <pre>{@code
 * SELECT step, balance
 * FROM DUAL
 * MODEL
 *   DIMENSION BY (1 AS step)
 *   MEASURES (100 AS balance)
 *   RULES ITERATE (10) UNTIL (PREVIOUS(balance[1]) = balance[1]) (
 *     balance[1] = ROUND(balance[1] * 0.9)
 *   )
 * }</pre>
 */
public non-sealed interface PreviousModelValueExpr extends Expression {
    /**
     * Creates an immutable PreviousModelValueExpr with its complete state.
     *
     * @param cell cell from the previous iteration
     * @return immutable node
     */
    static PreviousModelValueExpr of(CellRefExpr cell) {
        return new Impl(cell);
    }

    /**
     * Returns the cell from the previous iteration.
     *
     * @return cell from the previous iteration
     */
    CellRefExpr cell();

    /** {@inheritDoc} */
    @Override
    default <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitPreviousModelValueExpr(this);
    }

    /**
     * Immutable implementation.
     *
     * @param cell cell from the previous iteration
     */
    record Impl(CellRefExpr cell) implements PreviousModelValueExpr {
        /**
         * Validates and copies the supplied state.
         *
         * @param cell cell from the previous iteration
         */
        public Impl {
            Objects.requireNonNull(cell, "cell");
        }
    }
}
