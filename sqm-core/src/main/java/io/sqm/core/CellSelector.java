package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;
import io.sqm.core.match.CellSelectorMatch;
import java.util.Objects;

/**
 * A positional value or predicate selecting model cells.
 *
 * <p>The bracket entries {@code ANY} and {@code sales_year = 2026} are separate selectors. A positional
 * value such as {@code 'Bike'} is another variant.</p>
 * <pre>{@code
 * SELECT product, sales_year, sales
 * FROM sales_data
 * MODEL
 *   DIMENSION BY (product, sales_year)
 *   MEASURES (amount AS sales)
 *   RULES (
 *     sales[ANY, sales_year = 2026] = 100
 *   )
 * }</pre>
 */
public sealed interface CellSelector extends CellAddress {
    /**
     * Creates a matcher for this variant family.
     * @param <R> result type
     * @return variant matcher
     */
    default <R> CellSelectorMatch<R> matchCellSelector() {
        return CellSelectorMatch.match(this);
    }

    /**
     * A positional dimension value.
     *
     * <p>{@code 'Bike'} is the first selector in the assignment below.</p>
     * <pre>{@code
     * SELECT product, sales_year, sales
     * FROM sales_data
     * MODEL
     *   DIMENSION BY (product, sales_year)
     *   MEASURES (amount AS sales)
     *   RULES (
     *     sales['Bike', 2026] = 100
     *   )
     * }</pre>
     * @param value positional dimension value
     */
    record Value(Expression value) implements CellSelector {
        /**
         * Validates and copies this variant.
         * @param value positional dimension value
         */
        public Value {
            Objects.requireNonNull(value, "value");
        }

        /**
         * Creates this immutable variant.
         * @param value positional dimension value
         * @return immutable variant
         */
        public static Value of(Expression value) {
            return new Value(value);
        }

        /** {@inheritDoc} */
        @Override
        public <R> R accept(NodeVisitor<R> visitor) {
            return visitor.visitCellSelectorValue(this);
        }
    }

    /**
     * A complete predicate used as a symbolic cell selector.
     *
     * <p>This selector represents {@code sales_year = 2026}, including the dimension name.</p>
     * <pre>{@code
     * SELECT product, sales_year, sales
     * FROM sales_data
     * MODEL
     *   DIMENSION BY (product, sales_year)
     *   MEASURES (amount AS sales)
     *   RULES (
     *     sales['Bike', sales_year = 2026] = 100
     *   )
     * }</pre>
     * @param predicate complete selector predicate
     */
    record Condition(Predicate predicate) implements CellSelector {
        /**
         * Validates and copies this variant.
         * @param predicate complete selector predicate
         */
        public Condition {
            Objects.requireNonNull(predicate, "predicate");
        }

        /**
         * Creates this immutable variant.
         * @param predicate complete selector predicate
         * @return immutable variant
         */
        public static Condition of(Predicate predicate) {
            return new Condition(predicate);
        }

        /** {@inheritDoc} */
        @Override
        public <R> R accept(NodeVisitor<R> visitor) {
            return visitor.visitCellSelectorCondition(this);
        }
    }

}
