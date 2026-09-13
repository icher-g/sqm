package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;
import java.util.Objects;

/**
 * A conditional value based on an input cell's presence.
 *
 * <p>This node represents the {@code PRESENTV(...)} call below. {@link PresenceMode#CELL} maps to {@code
 * PRESENTV}; {@link PresenceMode#NON_NULL_VALUE} maps to {@code PRESENTNNV} with the same three-argument
 * shape, additionally requiring a non-null input value.</p>
 * <pre>{@code
 * SELECT product, sales_year, sales
 * FROM sales_data
 * MODEL
 *   DIMENSION BY (product, sales_year)
 *   MEASURES (amount AS sales)
 *   RULES (
 *     sales['Bike', 2026] = PRESENTV(sales['Bike', 2025], 100, 0)
 *   )
 * }</pre>
 */
public non-sealed interface PresenceValueExpr extends Expression {
    /**
     * Creates an immutable PresenceValueExpr with its complete state.
     *
     * @param mode presence test mode
     * @param cell cell whose presence is tested
     * @param whenPresent value when present
     * @param whenAbsent value when absent
     * @return immutable node
     */
    static PresenceValueExpr of(PresenceMode mode, CellRefExpr cell, Expression whenPresent, Expression whenAbsent) {
        return new Impl(mode, cell, whenPresent, whenAbsent);
    }

    /**
     * Returns the presence test mode.
     *
     * @return presence test mode
     */
    PresenceMode mode();

    /**
     * Returns the cell whose presence is tested.
     *
     * @return cell whose presence is tested
     */
    CellRefExpr cell();

    /**
     * Returns the value when present.
     *
     * @return value when present
     */
    Expression whenPresent();

    /**
     * Returns the value when absent.
     *
     * @return value when absent
     */
    Expression whenAbsent();

    /** {@inheritDoc} */
    @Override
    default <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitPresenceValueExpr(this);
    }

    /**
     * Immutable implementation.
     *
     * @param mode presence test mode
     * @param cell cell whose presence is tested
     * @param whenPresent value when present
     * @param whenAbsent value when absent
     */
    record Impl(PresenceMode mode, CellRefExpr cell, Expression whenPresent, Expression whenAbsent) implements PresenceValueExpr {
        /**
         * Validates and copies the supplied state.
         *
         * @param mode presence test mode
         * @param cell cell whose presence is tested
         * @param whenPresent value when present
         * @param whenAbsent value when absent
         */
        public Impl {
            Objects.requireNonNull(mode, "mode");
            Objects.requireNonNull(cell, "cell");
            Objects.requireNonNull(whenPresent, "whenPresent");
            Objects.requireNonNull(whenAbsent, "whenAbsent");
        }
    }
}
