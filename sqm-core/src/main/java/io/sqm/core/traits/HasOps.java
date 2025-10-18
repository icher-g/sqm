package io.sqm.core.traits;

import io.sqm.core.CompositeQuery;
import io.sqm.core.Query;
import io.sqm.core.views.Queries;

import java.util.List;

/**
 * An interface to get access to {@link CompositeQuery#ops()} from the {@link Query} interface.
 * Use {@link Queries#ops(Query)} to get them.
 */
public interface HasOps {
    List<CompositeQuery.Op> ops();
}
