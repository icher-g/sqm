package io.sqm.core.traits;

import io.sqm.core.CompositeQuery;
import io.sqm.core.Query;
import io.sqm.core.SelectQuery;
import io.sqm.core.views.Queries;

/**
 * An interface to get access to {@link SelectQuery#offset()} or {@link CompositeQuery#offset()}.
 * Use {@link Queries#offset(Query)} to get it.
 */
public interface HasOffset {
    Long offset();
}
