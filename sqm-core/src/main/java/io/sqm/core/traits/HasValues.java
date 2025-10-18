package io.sqm.core.traits;

import io.sqm.core.ColumnFilter;
import io.sqm.core.Filter;
import io.sqm.core.TupleFilter;
import io.sqm.core.Values;
import io.sqm.core.views.Filters;

/**
 * An interface to get access to {@link ColumnFilter#values()} or {@link TupleFilter#values()}.
 * Use {@link Filters#values(Filter)} to get access to them.
 */
public interface HasValues {
    Values values();
}
