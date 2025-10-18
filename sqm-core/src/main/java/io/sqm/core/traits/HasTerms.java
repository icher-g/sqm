package io.sqm.core.traits;

import io.sqm.core.Query;
import io.sqm.core.CompositeQuery;
import io.sqm.core.views.Queries;

import java.util.List;

/**
 * An interface to get access to {@link CompositeQuery#terms()} from the {@link Query} interface.
 * Use {@link Queries#terms(Query)}} to get it.
 */
public interface HasTerms {
    List<Query> terms();
}
