package io.sqm.core.walk;

import io.sqm.core.GroupBy;
import io.sqm.core.GroupItem;

/**
 * Visitor interface for traversing and processing grouping elements
 * within a SQL {@code GROUP BY} clause.
 * <p>
 * This visitor provides type-specific methods for visiting
 * {@link GroupBy} containers and their individual {@link GroupItem} elements.
 * It allows external logic such as validation, transformation,
 * or rendering to operate on grouping constructs without
 * embedding behavior inside the model itself.
 * </p>
 *
 * @param <R> the result type produced by the visitor methods,
 *            or {@link Void} if the visitor performs only side effects
 *
 * @see GroupBy
 * @see GroupItem
 */
public interface GroupVisitor<R> {

    /**
     * Visits an entire {@link GroupBy} clause.
     * <p>
     * Typical implementations will iterate over the contained
     * {@link GroupItem} elements and may apply dialect-specific
     * normalization or validation.
     * </p>
     *
     * @param g the {@code GROUP BY} node being visited (never {@code null})
     * @return a result value, or {@code null} if {@code <R>} is {@link Void}
     */
    R visitGroupBy(GroupBy g);

    /**
     * Visits a single {@link GroupItem}, representing an individual
     * grouping expression within a {@code GROUP BY} clause.
     *
     * @param i the grouping item being visited (never {@code null})
     * @return a result value, or {@code null} if {@code <R>} is {@link Void}
     */
    R visitGroupItem(GroupItem i);
}
