package io.cherlabs.sqlmodel.core.traits;

import io.cherlabs.sqlmodel.core.ColumnFilter;
import io.cherlabs.sqlmodel.core.Filter;
import io.cherlabs.sqlmodel.core.TupleFilter;
import io.cherlabs.sqlmodel.core.Values;

/**
 * An interface to get access to {@link ColumnFilter#values()} or {@link TupleFilter#values()}.
 * Use {@link io.cherlabs.sqlmodel.core.views.Filters#values(Filter)} to get access to them.
 */
public interface HasValues {
    Values values();
}
