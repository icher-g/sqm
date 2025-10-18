package io.sqm.core.traits;

import io.sqm.core.CompositeQuery;
import io.sqm.core.Query;
import io.sqm.core.SelectQuery;
import io.sqm.core.views.Queries;

/**
 * An interface to get access to {@link SelectQuery#limit()} or {@link CompositeQuery#limit()}.
 * Use {@link Queries#limit(Query)} to get it.
 */
public interface HasLimit {
    Long limit();
}
