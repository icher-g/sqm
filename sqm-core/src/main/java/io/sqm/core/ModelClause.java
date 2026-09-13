package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;
import java.util.List;
import java.util.Objects;

/**
 * A multidimensional calculation attached to a SELECT query.
 *
 * <p>This node represents the entire {@code MODEL} clause, from {@code MODEL} through the final rule
 * list.</p>
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
public non-sealed interface ModelClause extends Node {
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
    static Builder builder(ModelClause source) {
        return new Builder(source);
    }

    /** Mutable builder for an immutable {@link ModelClause}. */
    final class Builder {
        private ReturnRows returnRows = ReturnRows.ALL;
        private List<ReferenceModel> references = new java.util.ArrayList<>();
        private MainModel.Builder main = MainModel.builder();
        private Builder() {
        }

        private Builder(ModelClause source) {
            Objects.requireNonNull(source, "source");
            this.returnRows = source.returnRows();
            this.references = new java.util.ArrayList<>(source.references());
            this.main = MainModel.builder(source.main());
        }

        /**
         * Sets the result row selection.
         * @param returnRows result row selection
         * @return this builder
         */
        public Builder returnRows(ReturnRows returnRows) {
            this.returnRows = Objects.requireNonNull(returnRows, "returnRows");
            return this;
        }

        /**
         * Sets the read-only reference models.
         * @param references read-only reference models
         * @return this builder
         */
        public Builder references(List<ReferenceModel> references) {
            this.references = new java.util.ArrayList<>(List.copyOf(references));
            return this;
        }

        /**
         * Sets the main model.
         * @param main main model
         * @return this builder
         */
        public Builder main(MainModel main) {
            this.main = MainModel.builder(main);
            return this;
        }

        /**
         * Adds a model assignment.
         * @param rule assignment
         * @return this builder
         */
        public Builder rule(ModelRule rule) {
            main.rule(rule);
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
         * Adds a read-only reference model.
         * @param reference reference model
         * @return this builder
         */
        public Builder reference(ReferenceModel reference) {
            references.add(Objects.requireNonNull(reference, "reference"));
            return this;
        }

        /**
         * Adds a main-model partition.
         * @param declaration declaration
         * @return this builder
         */
        public Builder partition(ModelColumn declaration) {
            main.partition(declaration);
            return this;
        }

        /**
         * Adds a main-model partition.
         * @param column column
         * @return this builder
         */
        public Builder partition(String column) {
            main.partition(column);
            return this;
        }

        /**
         * Adds a main-model partition.
         * @param column column
         * @param name name
         * @return this builder
         */
        public Builder partition(String column, String name) {
            main.partition(column, name);
            return this;
        }

        /**
         * Adds a main-model partition.
         * @param column column
         * @return this builder
         */
        public Builder partition(ColumnExpr column) {
            main.partition(column);
            return this;
        }

        /**
         * Adds a main-model partition.
         * @param expression expression
         * @param name name
         * @return this builder
         */
        public Builder partition(Expression expression, String name) {
            main.partition(expression, name);
            return this;
        }

        /**
         * Adds a main-model partition.
         * @param expression expression
         * @param name name
         * @return this builder
         */
        public Builder partition(Expression expression, Identifier name) {
            main.partition(expression, name);
            return this;
        }

        /**
         * Adds a main-model dimension.
         * @param declaration declaration
         * @return this builder
         */
        public Builder dimension(ModelColumn declaration) {
            main.dimension(declaration);
            return this;
        }

        /**
         * Adds a main-model dimension.
         * @param column column
         * @return this builder
         */
        public Builder dimension(String column) {
            main.dimension(column);
            return this;
        }

        /**
         * Adds a main-model dimension.
         * @param column column
         * @param name name
         * @return this builder
         */
        public Builder dimension(String column, String name) {
            main.dimension(column, name);
            return this;
        }

        /**
         * Adds a main-model dimension.
         * @param column column
         * @return this builder
         */
        public Builder dimension(ColumnExpr column) {
            main.dimension(column);
            return this;
        }

        /**
         * Adds a main-model dimension.
         * @param expression expression
         * @param name name
         * @return this builder
         */
        public Builder dimension(Expression expression, String name) {
            main.dimension(expression, name);
            return this;
        }

        /**
         * Adds a main-model dimension.
         * @param expression expression
         * @param name name
         * @return this builder
         */
        public Builder dimension(Expression expression, Identifier name) {
            main.dimension(expression, name);
            return this;
        }

        /**
         * Adds a main-model measure.
         * @param declaration declaration
         * @return this builder
         */
        public Builder measure(ModelColumn declaration) {
            main.measure(declaration);
            return this;
        }

        /**
         * Adds a main-model measure.
         * @param column column
         * @return this builder
         */
        public Builder measure(String column) {
            main.measure(column);
            return this;
        }

        /**
         * Adds a main-model measure.
         * @param column column
         * @param name name
         * @return this builder
         */
        public Builder measure(String column, String name) {
            main.measure(column, name);
            return this;
        }

        /**
         * Adds a main-model measure.
         * @param column column
         * @return this builder
         */
        public Builder measure(ColumnExpr column) {
            main.measure(column);
            return this;
        }

        /**
         * Adds a main-model measure.
         * @param expression expression
         * @param name name
         * @return this builder
         */
        public Builder measure(Expression expression, String name) {
            main.measure(expression, name);
            return this;
        }

        /**
         * Adds a main-model measure.
         * @param expression expression
         * @param name name
         * @return this builder
         */
        public Builder measure(Expression expression, Identifier name) {
            main.measure(expression, name);
            return this;
        }

        /**
         * Sets main-model rule options and assignments.
         * @param rules complete rules section
         * @return this builder
         */
        public Builder rules(ModelRules rules) {
            main.rules(rules);
            return this;
        }

        /**
         * Builds the immutable model node.
         * @return immutable node
         */
        public ModelClause build() {
            return ModelClause.of(returnRows, references, main.build());
        }
    }
    /**
     * Creates an immutable ModelClause with its complete state.
     *
     * @param returnRows result row selection
     * @param references read-only reference models
     * @param main main model
     * @return immutable node
     */
    static ModelClause of(ReturnRows returnRows, List<ReferenceModel> references, MainModel main) {
        return new Impl(returnRows, references, main);
    }

    /**
     * Returns the result row selection.
     *
     * @return result row selection
     */
    ReturnRows returnRows();

    /**
     * Returns the read-only reference models.
     *
     * @return read-only reference models
     */
    List<ReferenceModel> references();

    /**
     * Returns the main model.
     *
     * @return main model
     */
    MainModel main();

    /** {@inheritDoc} */
    @Override
    default <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitModelClause(this);
    }

    /**
     * Immutable implementation.
     *
     * @param returnRows result row selection
     * @param references read-only reference models
     * @param main main model
     */
    record Impl(ReturnRows returnRows, List<ReferenceModel> references, MainModel main) implements ModelClause {
        /**
         * Validates and copies the supplied state.
         *
         * @param returnRows result row selection
         * @param references read-only reference models
         * @param main main model
         */
        public Impl {
            Objects.requireNonNull(returnRows, "returnRows");
            references = List.copyOf(references);
            Objects.requireNonNull(main, "main");
        }
    }
}
