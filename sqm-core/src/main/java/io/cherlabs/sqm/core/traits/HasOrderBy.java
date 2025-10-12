package io.cherlabs.sqm.core.traits;

import io.cherlabs.sqm.core.CompositeQuery;
import io.cherlabs.sqm.core.Order;
import io.cherlabs.sqm.core.Query;
import io.cherlabs.sqm.core.SelectQuery;

import java.util.List;

/**
 * An interface to get access to {@link SelectQuery#orderBy()} or {@link CompositeQuery#orderBy()} from the {@link Query} interface.
 * Use {@link io.cherlabs.sqm.core.views.Queries#orderBy(Query)} to get them.
 */
public interface HasOrderBy {
    List<Order> orderBy();
}
