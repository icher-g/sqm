package io.cherlabs.sqm.core.traits;

import io.cherlabs.sqm.core.Query;
import io.cherlabs.sqm.core.WithQuery;

/**
 * An interface to get access to {@link WithQuery#recursive()}
 * Use {@link io.cherlabs.sqm.core.views.Queries#recursive(Query)} to get it.
 */
public interface HasRecursive {
    boolean recursive();
}
