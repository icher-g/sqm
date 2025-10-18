package io.sqm.core.traits;

import io.sqm.core.Column;
import io.sqm.core.Filter;
import io.sqm.core.views.Filters;

/**
 * An interface to access {@link Column} on a {@link Filter}.
 * Use {@link Filters#column(Filter)} to get the column.
 */
public interface HasColumn {
    Column column();
}
