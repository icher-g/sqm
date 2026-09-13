package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;
import java.util.List;
import java.util.Objects;

/**
 * A named read-only model backed by a query.
 *
 * <p>This node represents {@code REFERENCE baseline ON (...)} and its dimension and measure declarations.
 * The main rule reads it through {@code baseline.sales['Bike']}.</p>
 * <pre>{@code
 * SELECT product, sales_year, sales
 * FROM sales_data
 * MODEL
 *   REFERENCE baseline ON (
 *     SELECT product, amount FROM baseline_sales
 *   )
 *     DIMENSION BY (product)
 *     MEASURES (amount AS sales)
 *   MAIN forecast
 *     DIMENSION BY (product, sales_year)
 *     MEASURES (amount AS sales)
 *     RULES (
 *       sales['Bike', 2026] = baseline.sales['Bike'] * 1.1
 *     )
 * }</pre>
 */
public non-sealed interface ReferenceModel extends Node {
    /**
     * Creates a builder with default MODEL options.
     * @return new builder
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a builder initialized from an existing node.
     * @param source node to copy
     * @return initialized builder
     */
    static Builder builder(ReferenceModel source) {
        return new Builder(source);
    }

    /** Mutable builder for an immutable {@link ReferenceModel}. */
    final class Builder {
        private Identifier name;
        private Query query;
        private List<ModelColumn> dimensions = new java.util.ArrayList<>();
        private List<ModelColumn> measures = new java.util.ArrayList<>();
        private NavigationMode navigationMode = NavigationMode.KEEP;
        private UniquenessMode uniquenessMode = UniquenessMode.DIMENSION;
        private Builder() {
        }

        private Builder(ReferenceModel source) {
            Objects.requireNonNull(source, "source");
            this.name = source.name();
            this.query = source.query();
            this.dimensions = new java.util.ArrayList<>(source.dimensions());
            this.measures = new java.util.ArrayList<>(source.measures());
            this.navigationMode = source.navigationMode();
            this.uniquenessMode = source.uniquenessMode();
        }

        /**
         * Sets the reference-model name.
         * @param name reference-model name
         * @return this builder
         */
        public Builder name(Identifier name) {
            this.name = Objects.requireNonNull(name, "name");
            return this;
        }

        /**
         * Sets the reference input query.
         * @param query reference input query
         * @return this builder
         */
        public Builder query(Query query) {
            this.query = Objects.requireNonNull(query, "query");
            return this;
        }

        /**
         * Sets the non-empty dimensions.
         * @param dimensions non-empty dimensions
         * @return this builder
         */
        public Builder dimensions(List<ModelColumn> dimensions) {
            this.dimensions = new java.util.ArrayList<>(List.copyOf(dimensions));
            return this;
        }

        /**
         * Sets the non-empty measures.
         * @param measures non-empty measures
         * @return this builder
         */
        public Builder measures(List<ModelColumn> measures) {
            this.measures = new java.util.ArrayList<>(List.copyOf(measures));
            return this;
        }

        /**
         * Sets the missing and null value behavior.
         * @param navigationMode missing and null value behavior
         * @return this builder
         */
        public Builder navigationMode(NavigationMode navigationMode) {
            this.navigationMode = Objects.requireNonNull(navigationMode, "navigationMode");
            return this;
        }

        /**
         * Sets the cell uniqueness policy.
         * @param uniquenessMode cell uniqueness policy
         * @return this builder
         */
        public Builder uniquenessMode(UniquenessMode uniquenessMode) {
            this.uniquenessMode = Objects.requireNonNull(uniquenessMode, "uniquenessMode");
            return this;
        }

        /**
         * Adds a dimension declaration.
         * @param declaration typed declaration
         * @return this builder
         */
        public Builder dimension(ModelColumn declaration) {
            this.dimensions.add(Objects.requireNonNull(declaration, "declaration"));
            return this;
        }

        /**
         * Adds a dimension declaration.
         * @param column unqualified source column
         * @return this builder
         */
        public Builder dimension(String column) {
            return dimension(ColumnExpr.of(null, Identifier.of(column)));
        }

        /**
         * Adds a dimension declaration.
         * @param column unqualified source column
         * @param name resolved model name
         * @return this builder
         */
        public Builder dimension(String column, String name) {
            return dimension(ColumnExpr.of(null, Identifier.of(column)), Identifier.of(name));
        }

        /**
         * Adds a dimension declaration.
         * @param column source column retaining qualification and quotes
         * @return this builder
         */
        public Builder dimension(ColumnExpr column) {
            return dimension(column, column.name());
        }

        /**
         * Adds a dimension declaration.
         * @param expression source expression
         * @param name resolved model name
         * @return this builder
         */
        public Builder dimension(Expression expression, String name) {
            return dimension(expression, Identifier.of(name));
        }

        /**
         * Adds a dimension declaration.
         * @param expression source expression
         * @param name resolved quote-aware model name
         * @return this builder
         */
        public Builder dimension(Expression expression, Identifier name) {
            return dimension(ModelColumn.of(expression, name));
        }

        /**
         * Adds a measure declaration.
         * @param declaration typed declaration
         * @return this builder
         */
        public Builder measure(ModelColumn declaration) {
            this.measures.add(Objects.requireNonNull(declaration, "declaration"));
            return this;
        }

        /**
         * Adds a measure declaration.
         * @param column unqualified source column
         * @return this builder
         */
        public Builder measure(String column) {
            return measure(ColumnExpr.of(null, Identifier.of(column)));
        }

        /**
         * Adds a measure declaration.
         * @param column unqualified source column
         * @param name resolved model name
         * @return this builder
         */
        public Builder measure(String column, String name) {
            return measure(ColumnExpr.of(null, Identifier.of(column)), Identifier.of(name));
        }

        /**
         * Adds a measure declaration.
         * @param column source column retaining qualification and quotes
         * @return this builder
         */
        public Builder measure(ColumnExpr column) {
            return measure(column, column.name());
        }

        /**
         * Adds a measure declaration.
         * @param expression source expression
         * @param name resolved model name
         * @return this builder
         */
        public Builder measure(Expression expression, String name) {
            return measure(expression, Identifier.of(name));
        }

        /**
         * Adds a measure declaration.
         * @param expression source expression
         * @param name resolved quote-aware model name
         * @return this builder
         */
        public Builder measure(Expression expression, Identifier name) {
            return measure(ModelColumn.of(expression, name));
        }

        /**
         * Builds the immutable model node.
         * @return immutable node
         */
        public ReferenceModel build() {
            return ReferenceModel.of(name, query, dimensions, measures, navigationMode, uniquenessMode);
        }
    }
    /**
     * Creates an immutable ReferenceModel with its complete state.
     *
     * @param name reference-model name
     * @param query reference input query
     * @param dimensions non-empty dimensions
     * @param measures non-empty measures
     * @param navigationMode missing and null value behavior
     * @param uniquenessMode cell uniqueness policy
     * @return immutable node
     */
    static ReferenceModel of(Identifier name, Query query, List<ModelColumn> dimensions, List<ModelColumn> measures, NavigationMode navigationMode, UniquenessMode uniquenessMode) {
        return new Impl(name, query, dimensions, measures, navigationMode, uniquenessMode);
    }

    /**
     * Returns the reference-model name.
     *
     * @return reference-model name
     */
    Identifier name();

    /**
     * Returns the reference input query.
     *
     * @return reference input query
     */
    Query query();

    /**
     * Returns the non-empty dimensions.
     *
     * @return non-empty dimensions
     */
    List<ModelColumn> dimensions();

    /**
     * Returns the non-empty measures.
     *
     * @return non-empty measures
     */
    List<ModelColumn> measures();

    /**
     * Returns the missing and null value behavior.
     *
     * @return missing and null value behavior
     */
    NavigationMode navigationMode();

    /**
     * Returns the cell uniqueness policy.
     *
     * @return cell uniqueness policy
     */
    UniquenessMode uniquenessMode();

    /** {@inheritDoc} */
    @Override
    default <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitReferenceModel(this);
    }

    /**
     * Immutable implementation.
     *
     * @param name reference-model name
     * @param query reference input query
     * @param dimensions non-empty dimensions
     * @param measures non-empty measures
     * @param navigationMode missing and null value behavior
     * @param uniquenessMode cell uniqueness policy
     */
    record Impl(Identifier name, Query query, List<ModelColumn> dimensions, List<ModelColumn> measures, NavigationMode navigationMode, UniquenessMode uniquenessMode) implements ReferenceModel {
        /**
         * Validates and copies the supplied state.
         *
         * @param name reference-model name
         * @param query reference input query
         * @param dimensions non-empty dimensions
         * @param measures non-empty measures
         * @param navigationMode missing and null value behavior
         * @param uniquenessMode cell uniqueness policy
         */
        public Impl {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(query, "query");
            dimensions = List.copyOf(dimensions);
            if (dimensions.isEmpty()) {
                throw new IllegalArgumentException("dimensions must not be empty");
            }
            measures = List.copyOf(measures);
            if (measures.isEmpty()) {
                throw new IllegalArgumentException("measures must not be empty");
            }
            Objects.requireNonNull(navigationMode, "navigationMode");
            Objects.requireNonNull(uniquenessMode, "uniquenessMode");
        }
    }
}
