package io.sqm.core;

import io.sqm.core.internal.ModelChecks;
import io.sqm.core.match.CellForMatch;
import io.sqm.core.walk.NodeVisitor;

import java.util.List;
import java.util.Objects;

/**
 * A generator of dimension values or tuples for writable model addresses.
 *
 * <p>This node represents {@code FOR sales_year FROM 2026 TO 2028 INCREMENT 1}. List and query generators
 * use {@code FOR ... IN (...)} instead.</p>
 * <pre>{@code
 * SELECT product, sales_year, sales
 * FROM sales_data
 * MODEL
 *   DIMENSION BY (product, sales_year)
 *   MEASURES (amount AS sales)
 *   RULES (
 *     sales['Bike', FOR sales_year FROM 2026 TO 2028 INCREMENT 1] = 100
 *   )
 * }</pre>
 */
public sealed interface CellFor extends CellAddress {
    /**
     * Creates a matcher for this variant family.
     *
     * @param <R> result type
     * @return variant matcher
     */
    default <R> CellForMatch<R> matchCellFor() {
        return CellForMatch.match(this);
    }

    /**
     * Returns the ordered generated dimensions.
     *
     * @return dimension names
     */
    List<Identifier> dimensions();

    /**
     * A generator backed by explicit values, tuples, or a query.
     *
     * <p>This node represents {@code FOR sales_year IN (2026, 2027)}. Several dimensions use
     * {@code FOR (product, sales_year) IN (('Bike', 2026), ('Car', 2027))}; a query source uses
     * {@code FOR sales_year IN (SELECT forecast_year FROM forecast_years)}.</p>
     * <pre>{@code
     * SELECT product, sales_year, sales
     * FROM sales_data
     * MODEL
     *   DIMENSION BY (product, sales_year)
     *   MEASURES (amount AS sales)
     *   RULES (
     *     sales['Bike', FOR sales_year IN (2026, 2027)] = 100
     *   )
     * }</pre>
     *
     * <p>The source reuses {@link ValueSet}: {@link RowExpr} holds a scalar list for one
     * dimension, {@link RowListExpr} holds tuples matching the dimension count, and
     * {@link QueryExpr} supplies query-produced tuples. Query projection width and
     * dialect-specific restrictions require contextual validation.</p>
     *
     * @param dimensions non-empty ordered dimension names
     * @param values non-empty explicit values or a query producing dimension tuples
     */
    record Values(List<Identifier> dimensions, ValueSet values) implements CellFor {
        /**
         * Copies the dimensions and validates the source's structural shape.
         *
         * @param dimensions non-empty ordered dimension names
         * @param values non-empty explicit values or a query producing dimension tuples
         */
        public Values {
            dimensions = List.copyOf(dimensions);
            if (dimensions.isEmpty()) {
                throw new IllegalArgumentException("dimensions must not be empty");
            }
            Objects.requireNonNull(values, "values");
            switch (values) {
                case RowExpr row -> {
                    if (dimensions.size() != 1) {
                        throw new IllegalArgumentException("scalar values require exactly one dimension");
                    }
                    if (row.items().isEmpty()) {
                        throw new IllegalArgumentException("values must not be empty");
                    }
                }
                case RowListExpr tuples -> {
                    if (tuples.rows().isEmpty()) {
                        throw new IllegalArgumentException("tuples must not be empty");
                    }
                    for (var row : tuples.rows()) {
                        if (row.items().size() != dimensions.size()) {
                            throw new IllegalArgumentException("tuple width must match dimension count");
                        }
                    }
                }
                case QueryExpr query -> Objects.requireNonNull(query.subquery(), "subquery");
            }
        }

        /**
         * Creates this immutable variant.
         *
         * @param dimensions non-empty ordered dimension names
         * @param values non-empty explicit values or a query producing dimension tuples
         * @return immutable generator
         */
        public static Values of(List<Identifier> dimensions, ValueSet values) {
            return new Values(dimensions, values);
        }

        /** {@inheritDoc} */
        @Override
        public <R> R accept(NodeVisitor<R> visitor) {
            return visitor.visitCellForValues(this);
        }
    }

    /**
     * A generator stepping through a bounded dimension range.
     *
     * <p>This node represents {@code FOR sales_year FROM 2026 TO 2028 INCREMENT 1}. A descending range uses
     * {@code DECREMENT}; an optional template can use {@code FOR sales_year LIKE 'Y%' FROM 2026 TO 2028
     * INCREMENT 1} for string coordinates.</p>
     * <pre>{@code
     * SELECT product, sales_year, sales
     * FROM sales_data
     * MODEL
     *   DIMENSION BY (product, sales_year)
     *   MEASURES (amount AS sales)
     *   RULES (
     *     sales['Bike', FOR sales_year FROM 2026 TO 2028 INCREMENT 1] = 100
     *   )
     * }</pre>
     *
     * @param dimension   dimension name
     * @param likePattern optional range template; {@code null} when absent
     * @param from        inclusive start
     * @param to          inclusive end
     * @param direction   range direction
     * @param step        positive range step
     */
    record Range(Identifier dimension, Expression likePattern, Expression from, Expression to, RangeDirection direction, Expression step) implements CellFor {
        /**
         * Validates and copies this variant.
         *
         * @param dimension   dimension name
         * @param likePattern optional range template; {@code null} when absent
         * @param from        inclusive start
         * @param to          inclusive end
         * @param direction   range direction
         * @param step        positive range step
         */
        public Range {
            Objects.requireNonNull(dimension, "dimension");
            Objects.requireNonNull(from, "from");
            Objects.requireNonNull(to, "to");
            Objects.requireNonNull(direction, "direction");
            Objects.requireNonNull(step, "step");
            ModelChecks.positiveLiteral(step, false, "range step");
        }

        /**
         * Creates this immutable variant.
         *
         * @param dimension   dimension name
         * @param likePattern optional range template; {@code null} when absent
         * @param from        inclusive start
         * @param to          inclusive end
         * @param direction   range direction
         * @param step        positive range step
         * @return immutable variant
         */
        public static Range of(Identifier dimension, Expression likePattern, Expression from, Expression to, RangeDirection direction, Expression step) {
            return new Range(dimension, likePattern, from, to, direction, step);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <R> R accept(NodeVisitor<R> visitor) {
            return visitor.visitCellForRange(this);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<Identifier> dimensions() {
            return List.of(dimension);
        }
    }

}
