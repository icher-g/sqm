package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;
import io.sqm.core.internal.CellInputs;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The writable measure and addresses on the left of a model rule.
 *
 * <p>This node represents {@code sales['Bike', 2026]} to the left of {@code =}. Its addresses may also
 * contain {@link CellFor} generators.</p>
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
public non-sealed interface CellTarget extends Node {
    /**
     * Creates an immutable CellTarget with its complete state.
     *
     * @param measure writable measure name
     * @param addresses non-empty cell addresses
     * @return immutable node
     */
    static CellTarget of(Identifier measure, List<CellAddress> addresses) {
        return new Impl(measure, addresses);
    }

    /**
     * Returns the writable measure name.
     *
     * @return writable measure name
     */
    Identifier measure();

    /**
     * Returns the non-empty cell addresses.
     *
     * @return non-empty cell addresses
     */
    List<CellAddress> addresses();


    /**
     * Starts a cell builder. A measure and at least one address are required at build time.
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
    static Builder builder(CellTarget source) {
        return new Builder(Objects.requireNonNull(source, "source"));
    }

    /**
     * Fluent cell builder, for example
     * {@code CellTarget.builder().measure("sales").address("Bike").address(2026).build()}.
     * Each build creates an immutable snapshot; incomplete state is rejected by {@code build()}.
     */
    final class Builder {
        private Identifier measure;
        private final List<CellAddress> addresses = new ArrayList<>();

        private Builder() {
        }

        private Builder(CellTarget source) {
            measure = source.measure();
            addresses.addAll(source.addresses());
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
         * Appends an address. Strings and scalar values become literals; expressions stay
         * expressions and predicates become conditions. {@code null} means SQL NULL.
         * FOR generators are accepted as writable addresses.
         * @param input address, predicate, expression, or literal value
         * @return this builder
         * @throws IllegalArgumentException if input is an unsupported SQL node
         */
        public Builder address(Object input) {
            addresses.add(CellInputs.address(input));
            return this;
        }

        /** Replaces all addresses, copying the supplied list.
         * @param addresses typed addresses; empty is allowed until build time
         * @return this builder
         */
        public Builder addresses(List<CellAddress> addresses) {
            var copy = List.copyOf(addresses);
            this.addresses.clear();
            this.addresses.addAll(copy);
            return this;
        }

        /** Removes all addresses.
         * @return this builder
         */
        public Builder clearAddresses() {
            addresses.clear();
            return this;
        }

        /** Creates an immutable cell with a measure and non-empty addresses.
         * @return immutable cell
         * @throws NullPointerException if the measure is missing
         * @throws IllegalArgumentException if no addresses were added
         */
        public CellTarget build() {
            return CellTarget.of(measure, addresses);
        }
    }

    /** {@inheritDoc} */
    @Override
    default <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitCellTarget(this);
    }

    /**
     * Immutable implementation.
     *
     * @param measure writable measure name
     * @param addresses non-empty cell addresses
     */
    record Impl(Identifier measure, List<CellAddress> addresses) implements CellTarget {
        /**
         * Validates and copies the supplied state.
         *
         * @param measure writable measure name
         * @param addresses non-empty cell addresses
         */
        public Impl {
            Objects.requireNonNull(measure, "measure");
            addresses = List.copyOf(addresses);
            if (addresses.isEmpty()) {
                throw new IllegalArgumentException("addresses must not be empty");
            }
        }
    }
}
