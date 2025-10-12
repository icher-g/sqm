package io.cherlabs.sqm.core.traits;

import io.cherlabs.sqm.core.CteQuery;
import io.cherlabs.sqm.core.Query;
import io.cherlabs.sqm.core.WithQuery;

/**
 * An interface to get access to {@link CteQuery#body()} or to {@link WithQuery#body()} from the reference to a Query object.
 * Use {@link io.cherlabs.sqm.core.views.Queries#body(Query)} to get it.
 */
public interface HasBody {
    Query body();
}
