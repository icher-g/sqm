package io.cherlabs.sqlmodel.core.traits;

import io.cherlabs.sqlmodel.core.Column;
import io.cherlabs.sqlmodel.core.Filter;

import java.util.List;

/**
 * An interface to access {@link List<Column> columns} on a {@link io.cherlabs.sqlmodel.core.Filter}.
 * The is required to access columns from the {@link io.cherlabs.sqlmodel.core.TupleFilter}.
 * Use {@link io.cherlabs.sqlmodel.core.views.Filters#columns(Filter)} to get columns.
 */
public interface HasColumns {
    List<Column> columns();
}
