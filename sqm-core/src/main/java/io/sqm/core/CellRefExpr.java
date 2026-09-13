package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;
import io.sqm.core.internal.CellInputs;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A readable measure cell selected by dimension coordinates.
 *
 * <p>This node represents the right-hand {@code sales['Bike', 2025]}, including the measure name and
 * bracketed selectors. It is not the left-hand {@link CellTarget}.</p>
 * <pre>{@code
 * SELECT product, sales_year, sales
 * FROM sales_data
 * MODEL
 *   DIMENSION BY (product, sales_year)
 *   MEASURES (amount AS sales)
 *   RULES (
 *     sales['Bike', 2026] = sales['Bike', 2025] * 1.1
 *   )
 * }</pre>
 */
public non-sealed interface CellRefExpr extends Expression {
    /**
     * Creates an immutable CellRefExpr with its complete state.
     *
     * @param model optional model qualifier; {@code null} when absent
     * @param measure measure name
     * @param selectors non-empty cell selectors
     * @return immutable node
     */
    static CellRefExpr of(Identifier model, Identifier measure, List<CellSelector> selectors) {
        return new Impl(model, measure, selectors);
    }

    /**
     * Returns the optional model qualifier.
     *
     * @return optional model qualifier; {@code null} when absent
     */
    Identifier model();

    /**
     * Returns the measure name.
     *
     * @return measure name
     */
    Identifier measure();

    /**
     * Returns the non-empty cell selectors.
     *
     * @return non-empty cell selectors
     */
    List<CellSelector> selectors();


    /**
     * Starts a cell builder. A measure and at least one selector are required at build time.
     * @return empty builder
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Starts a builder initialized from an existing cell.
     * @param source cell to copy
     * @return independent builder
     */
    static Builder builder(CellRefExpr source) {
        return new Builder(Objects.requireNonNull(source, "source"));
    }

    /**
     * Fluent cell builder, for example
     * {@code CellRefExpr.builder().measure("sales").selector("Bike").selector(2026).build()}.
     * Each build creates an immutable snapshot; incomplete state is rejected by {@code build()}.
     */
    final class Builder {
        private Identifier model;
        private Identifier measure;
        private final List<CellSelector> selectors = new ArrayList<>();

        private Builder() {
        }

        private Builder(CellRefExpr source) {
            model = source.model();
            measure = source.measure();
            selectors.addAll(source.selectors());
        }

        /** Sets the model qualifier.
         * @param model qualifier; {@code null} removes it
         * @return this builder
         */
        public Builder model(Identifier model) {
            this.model = model;
            return this;
        }

        /** Sets an unquoted model qualifier.
         * @param model model name
         * @return this builder
         */
        public Builder model(String model) {
            return model(Identifier.of(model));
        }

        /** Removes the model qualifier.
         * @return this builder
         */
        public Builder clearModel() {
            model = null;
            return this;
        }

        /** Sets the measure name, preserving identifier quoting.
         * @param measure measure name
         * @return this builder
         */
        public Builder measure(Identifier measure) {
            this.measure = Objects.requireNonNull(measure, "measure");
            return this;
        }

        /** Sets an unquoted measure name.
         * @param measure measure name
         * @return this builder
         */
        public Builder measure(String measure) {
            return measure(Identifier.of(measure));
        }

        /**
         * Appends a selector. Strings and scalar values become literals; expressions stay
         * expressions and predicates become conditions. {@code null} means SQL NULL.
         * FOR generators are rejected because readable cells only accept selectors.
         * @param input selector, predicate, expression, or literal value
         * @return this builder
         * @throws IllegalArgumentException if input is an unsupported SQL node
         */
        public Builder selector(Object input) {
            selectors.add(CellInputs.selector(input));
            return this;
        }

        /** Replaces all selectors, copying the supplied list.
         * @param selectors typed selectors; empty is allowed until build time
         * @return this builder
         */
        public Builder selectors(List<CellSelector> selectors) {
            var copy = List.copyOf(selectors);
            this.selectors.clear();
            this.selectors.addAll(copy);
            return this;
        }

        /** Removes all selectors.
         * @return this builder
         */
        public Builder clearSelectors() {
            selectors.clear();
            return this;
        }

        /** Creates an immutable cell with a measure and non-empty selectors.
         * @return immutable cell
         * @throws NullPointerException if the measure is missing
         * @throws IllegalArgumentException if no selectors were added
         */
        public CellRefExpr build() {
            return CellRefExpr.of(model, measure, selectors);
        }
    }

    /** {@inheritDoc} */
    @Override
    default <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitCellRefExpr(this);
    }

    /**
     * Immutable implementation.
     *
     * @param model optional model qualifier; {@code null} when absent
     * @param measure measure name
     * @param selectors non-empty cell selectors
     */
    record Impl(Identifier model, Identifier measure, List<CellSelector> selectors) implements CellRefExpr {
        /**
         * Validates and copies the supplied state.
         *
         * @param model optional model qualifier; {@code null} when absent
         * @param measure measure name
         * @param selectors non-empty cell selectors
         */
        public Impl {
            Objects.requireNonNull(measure, "measure");
            selectors = List.copyOf(selectors);
            if (selectors.isEmpty()) {
                throw new IllegalArgumentException("selectors must not be empty");
            }
        }
    }
}
