package io.cherlabs.sqm.core.traits;

import io.cherlabs.sqm.core.*;

import java.util.List;

/**
 * An interface to get access to {@link TupleFilter#columns()} on a {@link io.cherlabs.sqm.core.Filter} interface or
 * to get access to {@link SelectQuery#columns()} on a {@link io.cherlabs.sqm.core.Query} interface.
 * Use {@link io.cherlabs.sqm.core.views.Filters#columns(Filter)} to get columns.
 * Use {@link io.cherlabs.sqm.core.views.Queries#columns(Query)} to get them.
 */
public interface HasColumns {
    List<Column> columns();
}
