package io.cherlabs.sqm.core.traits;

import io.cherlabs.sqm.core.CompositeQuery;
import io.cherlabs.sqm.core.Query;
import io.cherlabs.sqm.core.SelectQuery;

/**
 * An interface to get access to {@link SelectQuery#limit()} or {@link CompositeQuery#limit()}.
 * Use {@link io.cherlabs.sqm.core.views.Queries#limit(Query)} to get it.
 */
public interface HasLimit {
    Long limit();
}
