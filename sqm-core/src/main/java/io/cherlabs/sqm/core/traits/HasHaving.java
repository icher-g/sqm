package io.cherlabs.sqm.core.traits;

import io.cherlabs.sqm.core.Filter;
import io.cherlabs.sqm.core.Query;
import io.cherlabs.sqm.core.SelectQuery;

/**
 * An interface to get access to {@link SelectQuery#having()} from the {@link io.cherlabs.sqm.core.Query} interface.
 * Use {@link io.cherlabs.sqm.core.views.Queries#having(Query)} to get it.
 */
public interface HasHaving {
    Filter having();
}
