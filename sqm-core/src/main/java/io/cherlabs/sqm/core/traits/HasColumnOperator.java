package io.cherlabs.sqm.core.traits;

import io.cherlabs.sqm.core.ColumnFilter;
import io.cherlabs.sqm.core.Filter;

/**
 * An interface to access {@link io.cherlabs.sqm.core.ColumnFilter.Operator} on a {@link io.cherlabs.sqm.core.Filter}.
 * Use {@link io.cherlabs.sqm.core.views.Filters#columnOperator(Filter)} to get an op.
 */
public interface HasColumnOperator {
    ColumnFilter.Operator op();
}
