package io.cherlabs.sqlmodel.core.traits;

import io.cherlabs.sqlmodel.core.ColumnFilter;
import io.cherlabs.sqlmodel.core.Filter;

/**
 * An interface to access {@link io.cherlabs.sqlmodel.core.ColumnFilter.Operator} on a {@link io.cherlabs.sqlmodel.core.Filter}.
 * Use {@link io.cherlabs.sqlmodel.core.views.Filters#columnOperator(Filter)} to get an op.
 */
public interface HasColumnOperator {
    ColumnFilter.Operator op();
}
