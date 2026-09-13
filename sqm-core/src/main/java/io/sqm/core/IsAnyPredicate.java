package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

/**
 * A MODEL wildcard predicate matching every value of a dimension, including null.
 *
 * <p>With an explicit dimension, this node represents {@code product IS ANY}.
 * Without one, it represents bare {@code ANY}; the enclosing cell selector's
 * position determines the dimension. Both forms are MODEL wildcard conditions,
 * not positional equality tests.</p>
 *
 * <p>This differs from {@link AnyAllPredicate}, which compares a left-hand value
 * with values supplied by a query or another quantified source, for example
 * {@code price > ANY (SELECT price FROM offers)}. {@code IsAnyPredicate} has no
 * comparison operator or value source: it selects all values of one MODEL
 * dimension. It is not a general-purpose WHERE predicate and requires a valid
 * MODEL context.</p>
 *
 * <p>The first rule below uses {@code product IS ANY}; the second uses bare
 * {@code ANY}. Each wildcard includes null product values.</p>
 * <pre>{@code
 * SELECT product, sales_year, sales
 * FROM sales_data
 * MODEL
 *   DIMENSION BY (product, sales_year)
 *   MEASURES (amount AS sales)
 *   RULES (
 *     sales[product IS ANY, 2026] = 100,
 *     sales[ANY, 2027] = 200
 *   )
 * }</pre>
 */
public non-sealed interface IsAnyPredicate extends Predicate {
    /**
     * Creates a MODEL wildcard with its complete state.
     *
     * @param dimension explicit dimension expression, or {@code null} for bare ANY
     * @return immutable wildcard predicate
     */
    static IsAnyPredicate of(Expression dimension) {
        return new Impl(dimension);
    }

    /**
     * Returns the explicit dimension expression, if present.
     *
     * <p>Oracle requires this expression to identify a dimension in the current
     * model. An absent dimension is meaningful only as a complete cell-selector
     * condition, where its position supplies the dimension.</p>
     *
     * @return explicit dimension, or {@code null} for a position-inferred wildcard
     */
    Expression dimension();

    /** {@inheritDoc} */
    @Override
    default <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitIsAnyPredicate(this);
    }

    /**
     * Immutable wildcard implementation.
     *
     * @param dimension explicit dimension expression, or {@code null} for bare ANY
     */
    record Impl(Expression dimension) implements IsAnyPredicate {
    }
}
