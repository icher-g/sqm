package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;
import java.util.List;
import java.util.Objects;

/**
 * The writable model, including declarations and rules.
 *
 * <p>This node represents {@code MAIN forecast} and its declarations and rules below. The preceding {@code
 * REFERENCE baseline} is a separate {@link ReferenceModel}.</p>
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
public non-sealed interface MainModel extends Node {
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
    static Builder builder(MainModel source) {
        return new Builder(source);
    }

    /** Mutable builder for an immutable {@link MainModel}. */
    final class Builder {
        private Identifier name;
        private List<ModelColumn> partitions = new java.util.ArrayList<>();
        private List<ModelColumn> dimensions = new java.util.ArrayList<>();
        private List<ModelColumn> measures = new java.util.ArrayList<>();
        private NavigationMode navigationMode = NavigationMode.KEEP;
        private UniquenessMode uniquenessMode = UniquenessMode.DIMENSION;
        private ModelRules.Builder rules = ModelRules.builder();
        private Builder() {
        }

        private Builder(MainModel source) {
            Objects.requireNonNull(source, "source");
            this.name = source.name();
            this.partitions = new java.util.ArrayList<>(source.partitions());
            this.dimensions = new java.util.ArrayList<>(source.dimensions());
            this.measures = new java.util.ArrayList<>(source.measures());
            this.navigationMode = source.navigationMode();
            this.uniquenessMode = source.uniquenessMode();
            this.rules = ModelRules.builder(source.rules());
        }

        /**
         * Sets the optional main-model name.
         * @param name optional main-model name; {@code null} when absent
         * @return this builder
         */
        public Builder name(Identifier name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the model name.
         * @param name model name; {@code null} when absent
         * @return this builder
         */
        public Builder name(String name) {
            return name(name == null ? null : Identifier.of(name));
        }

        /**
         * Removes the optional main-model name.
         * @return this builder
         */
        public Builder clearName() {
            this.name = null;
            return this;
        }

        /**
         * Sets the partition declarations.
         * @param partitions partition declarations
         * @return this builder
         */
        public Builder partitions(List<ModelColumn> partitions) {
            this.partitions = new java.util.ArrayList<>(List.copyOf(partitions));
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
         * Sets the model rules.
         * @param rules model rules
         * @return this builder
         */
        public Builder rules(ModelRules rules) {
            this.rules = ModelRules.builder(rules);
            return this;
        }

        /**
         * Adds a partition declaration.
         * @param declaration typed declaration
         * @return this builder
         */
        public Builder partition(ModelColumn declaration) {
            this.partitions.add(Objects.requireNonNull(declaration, "declaration"));
            return this;
        }

        /**
         * Adds a partition declaration.
         * @param column unqualified source column
         * @return this builder
         */
        public Builder partition(String column) {
            return partition(ColumnExpr.of(null, Identifier.of(column)));
        }

        /**
         * Adds a partition declaration.
         * @param column unqualified source column
         * @param name resolved model name; {@code null} when absent
         * @return this builder
         */
        public Builder partition(String column, String name) {
            return partition(ColumnExpr.of(null, Identifier.of(column)), Identifier.of(name));
        }

        /**
         * Adds a partition declaration.
         * @param column source column retaining qualification and quotes
         * @return this builder
         */
        public Builder partition(ColumnExpr column) {
            return partition(column, column.name());
        }

        /**
         * Adds a partition declaration.
         * @param expression source expression
         * @param name resolved model name; {@code null} when absent
         * @return this builder
         */
        public Builder partition(Expression expression, String name) {
            return partition(expression, Identifier.of(name));
        }

        /**
         * Adds a partition declaration.
         * @param expression source expression
         * @param name resolved quote-aware model name; {@code null} when absent
         * @return this builder
         */
        public Builder partition(Expression expression, Identifier name) {
            return partition(ModelColumn.of(expression, name));
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
         * @param name resolved model name; {@code null} when absent
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
         * @param name resolved model name; {@code null} when absent
         * @return this builder
         */
        public Builder dimension(Expression expression, String name) {
            return dimension(expression, Identifier.of(name));
        }

        /**
         * Adds a dimension declaration.
         * @param expression source expression
         * @param name resolved quote-aware model name; {@code null} when absent
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
         * @param name resolved model name; {@code null} when absent
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
         * @param name resolved model name; {@code null} when absent
         * @return this builder
         */
        public Builder measure(Expression expression, String name) {
            return measure(expression, Identifier.of(name));
        }

        /**
         * Adds a measure declaration.
         * @param expression source expression
         * @param name resolved quote-aware model name; {@code null} when absent
         * @return this builder
         */
        public Builder measure(Expression expression, Identifier name) {
            return measure(ModelColumn.of(expression, name));
        }

        /**
         * Adds a model assignment.
         * @param rule assignment
         * @return this builder
         */
        public Builder rule(ModelRule rule) {
            rules.rule(rule);
            return this;
        }

        /**
         * Adds an assignment with inherited rule options.
         * @param target writable target
         * @param value assigned expression
         * @return this builder
         */
        public Builder rule(CellTarget target, Expression value) {
            return rule(ModelRule.of(null, target, null, value));
        }

        /**
         * Builds the immutable model node.
         * @return immutable node
         */
        public MainModel build() {
            return MainModel.of(name, partitions, dimensions, measures, navigationMode, uniquenessMode, rules.build());
        }
    }
    /**
     * Creates an immutable MainModel with its complete state.
     *
     * @param name optional main-model name; {@code null} when absent
     * @param partitions partition declarations
     * @param dimensions non-empty dimensions
     * @param measures non-empty measures
     * @param navigationMode missing and null value behavior
     * @param uniquenessMode cell uniqueness policy
     * @param rules model rules
     * @return immutable node
     */
    static MainModel of(Identifier name, List<ModelColumn> partitions, List<ModelColumn> dimensions, List<ModelColumn> measures, NavigationMode navigationMode, UniquenessMode uniquenessMode, ModelRules rules) {
        return new Impl(name, partitions, dimensions, measures, navigationMode, uniquenessMode, rules);
    }

    /**
     * Returns the optional main-model name.
     *
     * @return optional main-model name; {@code null} when absent
     */
    Identifier name();

    /**
     * Returns the partition declarations.
     *
     * @return partition declarations
     */
    List<ModelColumn> partitions();

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

    /**
     * Returns the model rules.
     *
     * @return model rules
     */
    ModelRules rules();

    /** {@inheritDoc} */
    @Override
    default <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitMainModel(this);
    }

    /**
     * Immutable implementation.
     *
     * @param name optional main-model name; {@code null} when absent
     * @param partitions partition declarations
     * @param dimensions non-empty dimensions
     * @param measures non-empty measures
     * @param navigationMode missing and null value behavior
     * @param uniquenessMode cell uniqueness policy
     * @param rules model rules
     */
    record Impl(Identifier name, List<ModelColumn> partitions, List<ModelColumn> dimensions, List<ModelColumn> measures, NavigationMode navigationMode, UniquenessMode uniquenessMode, ModelRules rules) implements MainModel {
        /**
         * Validates and copies the supplied state.
         *
         * @param name optional main-model name; {@code null} when absent
         * @param partitions partition declarations
         * @param dimensions non-empty dimensions
         * @param measures non-empty measures
         * @param navigationMode missing and null value behavior
         * @param uniquenessMode cell uniqueness policy
         * @param rules model rules
         */
        public Impl {
            partitions = List.copyOf(partitions);
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
            Objects.requireNonNull(rules, "rules");
        }
    }
}
