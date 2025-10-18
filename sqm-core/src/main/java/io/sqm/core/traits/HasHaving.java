package io.sqm.core.traits;

import io.sqm.core.Filter;
import io.sqm.core.Query;
import io.sqm.core.SelectQuery;
import io.sqm.core.views.Queries;

/**
 * An interface to get access to {@link SelectQuery#having()} from the {@link Query} interface.
 * Use {@link Queries#having(Query)} to get it.
 */
public interface HasHaving {
    Filter having();
}
