package io.cherlabs.sqm.core.traits;

import io.cherlabs.sqm.core.Group;
import io.cherlabs.sqm.core.Query;
import io.cherlabs.sqm.core.SelectQuery;

import java.util.List;

/**
 * An interface to get access to {@link SelectQuery#groupBy()} from the {@link io.cherlabs.sqm.core.Query} interface.
 * Use {@link io.cherlabs.sqm.core.views.Queries#groupBy(Query)} to get them.
 */
public interface HasGroupBy {
    List<Group> groupBy();
}
