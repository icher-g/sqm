package io.cherlabs.sqlmodel.core.traits;

import io.cherlabs.sqlmodel.core.Column;
import io.cherlabs.sqlmodel.core.Filter;

/**
 * An interface to access {@link Column} on a {@link io.cherlabs.sqlmodel.core.Filter}.
 * Use {@link io.cherlabs.sqlmodel.core.views.Filters#column(Filter)} to get the column.
 */
public interface HasColumn {
    Column column();
}
