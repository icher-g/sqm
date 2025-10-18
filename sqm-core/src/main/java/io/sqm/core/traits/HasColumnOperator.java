package io.sqm.core.traits;

import io.sqm.core.ColumnFilter;
import io.sqm.core.Filter;
import io.sqm.core.views.Filters;

/**
 * An interface to access {@link ColumnFilter.Operator} on a {@link Filter}.
 * Use {@link Filters#columnOperator(Filter)} to get an op.
 */
public interface HasColumnOperator {
    ColumnFilter.Operator op();
}
