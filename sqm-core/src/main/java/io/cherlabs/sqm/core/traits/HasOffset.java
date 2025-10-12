package io.cherlabs.sqm.core.traits;

import io.cherlabs.sqm.core.CompositeQuery;
import io.cherlabs.sqm.core.Query;
import io.cherlabs.sqm.core.SelectQuery;

/**
 * An interface to get access to {@link SelectQuery#offset()} or {@link CompositeQuery#offset()}.
 * Use {@link io.cherlabs.sqm.core.views.Queries#offset(Query)} to get it.
 */
public interface HasOffset {
    Long offset();
}
