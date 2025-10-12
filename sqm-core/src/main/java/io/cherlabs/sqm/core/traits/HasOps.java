package io.cherlabs.sqm.core.traits;

import io.cherlabs.sqm.core.CompositeQuery;
import io.cherlabs.sqm.core.Query;

import java.util.List;

/**
 * An interface to get access to {@link CompositeQuery#ops()} from the {@link io.cherlabs.sqm.core.Query} interface.
 * Use {@link io.cherlabs.sqm.core.views.Queries#ops(Query)} to get them.
 */
public interface HasOps {
    List<CompositeQuery.Op> ops();
}
