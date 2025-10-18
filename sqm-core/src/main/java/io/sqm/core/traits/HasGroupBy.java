package io.sqm.core.traits;

import io.sqm.core.GroupBy;
import io.sqm.core.Query;
import io.sqm.core.SelectQuery;
import io.sqm.core.views.Queries;

/**
 * An interface to get access to {@link SelectQuery#groupBy()} from the {@link Query} interface.
 * Use {@link Queries#groupBy(Query)} to get them.
 */
public interface HasGroupBy {
    GroupBy groupBy();
}
