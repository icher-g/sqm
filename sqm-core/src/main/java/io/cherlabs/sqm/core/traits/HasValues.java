package io.cherlabs.sqm.core.traits;

import io.cherlabs.sqm.core.ColumnFilter;
import io.cherlabs.sqm.core.Filter;
import io.cherlabs.sqm.core.TupleFilter;
import io.cherlabs.sqm.core.Values;

/**
 * An interface to get access to {@link ColumnFilter#values()} or {@link TupleFilter#values()}.
 * Use {@link io.cherlabs.sqm.core.views.Filters#values(Filter)} to get access to them.
 */
public interface HasValues {
    Values values();
}
