package io.sqm.core.traits;

import io.sqm.core.CteQuery;
import io.sqm.core.Query;
import io.sqm.core.WithQuery;
import io.sqm.core.views.Queries;

import java.util.List;

/**
 * An interface to get access to {@link WithQuery#ctes()} from the Query object.
 * Use {@link Queries#ctes(Query)} to get them.
 */
public interface HasCtes {
    List<CteQuery> ctes();
}
