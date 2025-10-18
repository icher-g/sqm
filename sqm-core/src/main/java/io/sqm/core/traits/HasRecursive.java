package io.sqm.core.traits;

import io.sqm.core.Query;
import io.sqm.core.WithQuery;
import io.sqm.core.views.Queries;

/**
 * An interface to get access to {@link WithQuery#recursive()}
 * Use {@link Queries#recursive(Query)} to get it.
 */
public interface HasRecursive {
    boolean recursive();
}
