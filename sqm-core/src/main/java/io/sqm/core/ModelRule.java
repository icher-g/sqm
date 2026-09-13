package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;
import java.util.Objects;

/**
 * One assignment to a model cell or set of cells.
 *
 * <p>This node represents the complete {@code UPDATE sales[ANY, 2026] = ...} assignment, including its mode
 * override, target, and value.</p>
 * <pre>{@code
 * SELECT product, sales_year, sales
 * FROM sales_data
 * MODEL
 *   DIMENSION BY (product, sales_year)
 *   MEASURES (amount AS sales)
 *   RULES UPSERT SEQUENTIAL ORDER (
 *     UPDATE sales[ANY, 2026] = sales[CV(product), 2025] * 1.1
 *   )
 * }</pre>
 */
public non-sealed interface ModelRule extends Node {
    /**
     * Creates an immutable ModelRule with its complete state.
     *
     * @param mode optional override of the rule mode; {@code null} when absent
     * @param target writable target
     * @param orderBy optional target evaluation order; {@code null} when absent
     * @param value assigned expression
     * @return immutable node
     */
    static ModelRule of(RuleMode mode, CellTarget target, OrderBy orderBy, Expression value) {
        return new Impl(mode, target, orderBy, value);
    }

    /**
     * Returns the optional override of the rule mode.
     *
     * @return optional override of the rule mode; {@code null} when absent
     */
    RuleMode mode();

    /**
     * Returns the writable target.
     *
     * @return writable target
     */
    CellTarget target();

    /**
     * Returns the optional target evaluation order.
     *
     * @return optional target evaluation order; {@code null} when absent
     */
    OrderBy orderBy();

    /**
     * Returns the assigned expression.
     *
     * @return assigned expression
     */
    Expression value();

    /** {@inheritDoc} */
    @Override
    default <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitModelRule(this);
    }

    /**
     * Immutable implementation.
     *
     * @param mode optional override of the rule mode; {@code null} when absent
     * @param target writable target
     * @param orderBy optional target evaluation order; {@code null} when absent
     * @param value assigned expression
     */
    record Impl(RuleMode mode, CellTarget target, OrderBy orderBy, Expression value) implements ModelRule {
        /**
         * Validates and copies the supplied state.
         *
         * @param mode optional override of the rule mode; {@code null} when absent
         * @param target writable target
         * @param orderBy optional target evaluation order; {@code null} when absent
         * @param value assigned expression
         */
        public Impl {
            Objects.requireNonNull(target, "target");
            Objects.requireNonNull(value, "value");
        }
    }
}
