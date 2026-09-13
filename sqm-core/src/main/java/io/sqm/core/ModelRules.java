package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;
import java.util.List;
import java.util.Objects;

/**
 * A model's assignment list and evaluation options.
 *
 * <p>This node represents {@code RULES UPSERT SEQUENTIAL ORDER (...)}: the default assignment mode, rule
 * ordering, and contained assignments.</p>
 * <pre>{@code
 * SELECT product, sales_year, sales
 * FROM sales_data
 * MODEL
 *   DIMENSION BY (product, sales_year)
 *   MEASURES (amount AS sales)
 *   RULES UPSERT SEQUENTIAL ORDER (
 *     sales['Bike', 2026] = sales['Bike', 2025] * 1.1
 *   )
 * }</pre>
 */
public non-sealed interface ModelRules extends Node {
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
    static Builder builder(ModelRules source) {
        return new Builder(source);
    }

    /** Mutable builder for an immutable {@link ModelRules}. */
    final class Builder {
        private RuleMode defaultMode = RuleMode.UPSERT;
        private RuleOrder order = RuleOrder.SEQUENTIAL;
        private IterationSpec iteration;
        private List<ModelRule> rules = new java.util.ArrayList<>();
        private Builder() {
        }

        private Builder(ModelRules source) {
            Objects.requireNonNull(source, "source");
            this.defaultMode = source.defaultMode();
            this.order = source.order();
            this.iteration = source.iteration();
            this.rules = new java.util.ArrayList<>(source.rules());
        }

        /**
         * Sets the default assignment mode.
         * @param defaultMode default assignment mode
         * @return this builder
         */
        public Builder defaultMode(RuleMode defaultMode) {
            this.defaultMode = Objects.requireNonNull(defaultMode, "defaultMode");
            return this;
        }

        /**
         * Sets the rule evaluation order.
         * @param order rule evaluation order
         * @return this builder
         */
        public Builder order(RuleOrder order) {
            this.order = Objects.requireNonNull(order, "order");
            return this;
        }

        /**
         * Sets the optional iteration specification.
         * @param iteration optional iteration specification; {@code null} when absent
         * @return this builder
         */
        public Builder iteration(IterationSpec iteration) {
            this.iteration = iteration;
            return this;
        }

        /**
         * Sets the non-empty rules.
         * @param rules non-empty rules
         * @return this builder
         */
        public Builder rules(List<ModelRule> rules) {
            this.rules = new java.util.ArrayList<>(List.copyOf(rules));
            return this;
        }

        /**
         * Adds a model assignment.
         * @param rule assignment
         * @return this builder
         */
        public Builder rule(ModelRule rule) {
            rules.add(Objects.requireNonNull(rule, "rule"));
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
        public ModelRules build() {
            return ModelRules.of(defaultMode, order, iteration, rules);
        }
    }
    /**
     * Creates an immutable ModelRules with its complete state.
     *
     * @param defaultMode default assignment mode
     * @param order rule evaluation order
     * @param iteration optional iteration specification; {@code null} when absent
     * @param rules non-empty rules
     * @return immutable node
     */
    static ModelRules of(RuleMode defaultMode, RuleOrder order, IterationSpec iteration, List<ModelRule> rules) {
        return new Impl(defaultMode, order, iteration, rules);
    }

    /**
     * Returns the default assignment mode.
     *
     * @return default assignment mode
     */
    RuleMode defaultMode();

    /**
     * Returns the rule evaluation order.
     *
     * @return rule evaluation order
     */
    RuleOrder order();

    /**
     * Returns the optional iteration specification.
     *
     * @return optional iteration specification; {@code null} when absent
     */
    IterationSpec iteration();

    /**
     * Returns the non-empty rules.
     *
     * @return non-empty rules
     */
    List<ModelRule> rules();

    /** {@inheritDoc} */
    @Override
    default <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitModelRules(this);
    }

    /**
     * Immutable implementation.
     *
     * @param defaultMode default assignment mode
     * @param order rule evaluation order
     * @param iteration optional iteration specification; {@code null} when absent
     * @param rules non-empty rules
     */
    record Impl(RuleMode defaultMode, RuleOrder order, IterationSpec iteration, List<ModelRule> rules) implements ModelRules {
        /**
         * Validates and copies the supplied state.
         *
         * @param defaultMode default assignment mode
         * @param order rule evaluation order
         * @param iteration optional iteration specification; {@code null} when absent
         * @param rules non-empty rules
         */
        public Impl {
            Objects.requireNonNull(defaultMode, "defaultMode");
            Objects.requireNonNull(order, "order");
            rules = List.copyOf(rules);
            if (rules.isEmpty()) {
                throw new IllegalArgumentException("rules must not be empty");
            }
        }
    }
}
