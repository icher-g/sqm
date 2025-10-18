package io.sqm.core.traits;

import io.sqm.core.CteQuery;
import io.sqm.core.Query;
import io.sqm.core.WithQuery;
import io.sqm.core.views.Queries;

/**
 * An interface to get access to {@link CteQuery#body()} or to {@link WithQuery#body()} from the reference to a Query object.
 * Use {@link Queries#body(Query)} to get it.
 */
public interface HasBody {
    Query body();
}
