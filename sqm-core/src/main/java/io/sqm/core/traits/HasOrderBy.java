package io.sqm.core.traits;

import io.sqm.core.CompositeQuery;
import io.sqm.core.OrderBy;
import io.sqm.core.Query;
import io.sqm.core.SelectQuery;
import io.sqm.core.views.Queries;

/**
 * An interface to get access to {@link SelectQuery#orderBy()} or {@link CompositeQuery#orderBy()} from the {@link Query} interface.
 * Use {@link Queries#orderBy(Query)} to get them.
 */
public interface HasOrderBy {
    OrderBy orderBy();
}
