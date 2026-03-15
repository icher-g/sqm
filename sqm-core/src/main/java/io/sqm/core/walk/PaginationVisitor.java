package io.sqm.core.walk;

import io.sqm.core.LimitOffset;
import io.sqm.core.TopSpec;

/**
 * Visitor interface for traversing pagination elements
 * such as {@link LimitOffset} within a SQL query.
 * <p>
 * Pagination constructs control result limits and offsets,
 * typically expressed as {@code LIMIT n OFFSET m} or
 * dialect-specific variants like {@code FETCH FIRST}.
 * </p>
 *
 * @param <R> the result type produced by the visitor methods,
 *            or {@link Void} if the visitor performs only side effects
 *
 * @see LimitOffset
 */
public interface PaginationVisitor<R> {

    /**
     * Visits a {@link LimitOffset} node that represents the
     * pagination segment of a SQL query.
     *
     * @param l the pagination node being visited (never {@code null})
     * @return a result value, or {@code null} if {@code <R>} is {@link Void}
     */
    R visitLimitOffset(LimitOffset l);

    /**
     * Visits a {@link TopSpec} node that represents a select-head row-limiting
     * construct such as SQL Server {@code TOP}.
     *
     * @param t top specification being visited
     * @return a result value, or {@code null} if {@code <R>} is {@link Void}
     */
    R visitTopSpec(TopSpec t);
}

