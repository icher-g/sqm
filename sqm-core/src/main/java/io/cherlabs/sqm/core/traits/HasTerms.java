package io.cherlabs.sqm.core.traits;

import io.cherlabs.sqm.core.Query;
import io.cherlabs.sqm.core.views.Queries;

import java.util.List;

/**
 * An interface to get access to {@link io.cherlabs.sqm.core.CompositeQuery#terms()} from the {@link Query} interface.
 * Use {@link Queries#terms(Query)}} to get it.
 */
public interface HasTerms {
    List<Query> terms();
}
