package io.cherlabs.sqm.core.traits;

import io.cherlabs.sqm.core.Column;
import io.cherlabs.sqm.core.Filter;

/**
 * An interface to access {@link Column} on a {@link io.cherlabs.sqm.core.Filter}.
 * Use {@link io.cherlabs.sqm.core.views.Filters#column(Filter)} to get the column.
 */
public interface HasColumn {
    Column column();
}
