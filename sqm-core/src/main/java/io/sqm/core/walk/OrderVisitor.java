package io.sqm.core.walk;

import io.sqm.core.OrderBy;
import io.sqm.core.OrderItem;

/**
 * Visitor interface for traversing and processing ordering elements
 * within a SQL {@code ORDER BY} clause.
 * <p>
 * This visitor provides type-specific methods for visiting
 * {@link OrderBy} containers and their individual {@link OrderItem} elements.
 * It enables custom logic such as rendering, sorting normalization,
 * or dialect validation to be implemented externally from the model.
 * </p>
 *
 * @param <R> the result type produced by the visitor methods,
 *            or {@link Void} if the visitor performs only side effects
 *
 * @see OrderBy
 * @see OrderItem
 */
public interface OrderVisitor<R> {

    /**
     * Visits an entire {@link OrderBy} clause.
     * <p>
     * Implementations typically traverse its {@link OrderItem} elements
     * and may apply formatting, validation, or ordering transformations.
     * </p>
     *
     * @param o the {@code ORDER BY} node being visited (never {@code null})
     * @return a result value, or {@code null} if {@code <R>} is {@link Void}
     */
    R visitOrderBy(OrderBy o);

    /**
     * Visits a single {@link OrderItem}, representing one
     * ordering expression (expr.g. {@code col DESC NULLS LAST})
     * within an {@code ORDER BY} clause.
     *
     * @param i the order item being visited (never {@code null})
     * @return a result value, or {@code null} if {@code <R>} is {@link Void}
     */
    R visitOrderItem(OrderItem i);
}

