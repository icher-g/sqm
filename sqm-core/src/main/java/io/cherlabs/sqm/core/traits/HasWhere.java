package io.cherlabs.sqm.core.traits;

import io.cherlabs.sqm.core.Filter;
import io.cherlabs.sqm.core.Query;
import io.cherlabs.sqm.core.SelectQuery;

/**
 * An interface to get access to {@link SelectQuery#where()} from the {@link io.cherlabs.sqm.core.Query} interface.
 * Use {@link io.cherlabs.sqm.core.views.Queries#where(Query)} to get it.
 */
public interface HasWhere {
    Filter where();
}
