package io.cherlabs.sqm.core.traits;

import io.cherlabs.sqm.core.CteQuery;
import io.cherlabs.sqm.core.Query;
import io.cherlabs.sqm.core.WithQuery;

import java.util.List;

/**
 * An interface to get access to {@link WithQuery#ctes()} from the Query object.
 * Use {@link io.cherlabs.sqm.core.views.Queries#ctes(Query)} to get them.
 */
public interface HasCtes {
    List<CteQuery> ctes();
}
