package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.Objects;

/**
 * An input expression declared under a resolved column name in a MODEL clause.
 *
 * <p>The containing model's partition, dimension, or measure list determines the
 * declaration's role. This node represents {@code country} in PARTITION BY,
 * {@code sales_year AS year_key} in DIMENSION BY, and {@code amount AS sales}
 * in MEASURES in the example below.</p>
 * <pre>{@code
 * SELECT country, product, year_key, sales
 * FROM sales_data
 * MODEL
 *   PARTITION BY (country)
 *   DIMENSION BY (product, sales_year AS year_key)
 *   MEASURES (amount AS sales)
 *   RULES (
 *     sales['Bike', 2026] = sales['Bike', 2025] * 1.1
 *   )
 * }</pre>
 *
 * <p>{@link #expression()} reads the input expression, such as {@code amount};
 * {@link #name()} exposes its resolved model-column name, such as {@code sales}.
 * For an unaliased column such as {@code country}, both use the same name.
 * Role-specific validation and transformation use the containing model list,
 * not a subtype or role flag on this declaration.</p>
 */
public non-sealed interface ModelColumn extends Node {
    /**
     * Creates an immutable model-column declaration.
     *
     * @param expression non-null input expression
     * @param name non-null resolved model-column name
     * @return immutable declaration
     */
    static ModelColumn of(Expression expression, Identifier name) {
        return new Impl(expression, name);
    }

    /**
     * Returns the input expression.
     * @return non-null input expression
     */
    Expression expression();

    /**
     * Returns the resolved model-column name.
     * @return non-null resolved name
     */
    Identifier name();

    /** {@inheritDoc} */
    @Override
    default <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitModelColumn(this);
    }

    /**
     * Immutable model-column declaration.
     *
     * @param expression non-null input expression
     * @param name non-null resolved model-column name
     */
    record Impl(Expression expression, Identifier name) implements ModelColumn {
        /**
         * Validates the required expression and name.
         *
         * @param expression non-null input expression
         * @param name non-null resolved model-column name
         */
        public Impl {
            Objects.requireNonNull(expression, "expression");
            Objects.requireNonNull(name, "name");
        }
    }
}
