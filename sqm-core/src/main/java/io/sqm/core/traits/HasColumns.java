package io.sqm.core.traits;

import io.sqm.core.*;
import io.sqm.core.views.Filters;
import io.sqm.core.views.Queries;

import java.util.List;

/**
 * An interface to get access to {@link TupleFilter#columns()} on a {@link Filter} interface or
 * to get access to {@link SelectQuery#columns()} on a {@link Query} interface.
 * Use {@link Filters#columns(Filter)} to get columns.
 * Use {@link Queries#columns(Query)} to get them.
 */
public interface HasColumns {
    List<Column> columns();
}
