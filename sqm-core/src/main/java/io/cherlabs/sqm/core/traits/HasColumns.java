package io.cherlabs.sqm.core.traits;

import io.cherlabs.sqm.core.Column;
import io.cherlabs.sqm.core.Filter;

import java.util.List;

/**
 * An interface to access {@link List<Column> columns} on a {@link io.cherlabs.sqm.core.Filter}.
 * The is required to access columns from the {@link io.cherlabs.sqm.core.TupleFilter}.
 * Use {@link io.cherlabs.sqm.core.views.Filters#columns(Filter)} to get columns.
 */
public interface HasColumns {
    List<Column> columns();
}
