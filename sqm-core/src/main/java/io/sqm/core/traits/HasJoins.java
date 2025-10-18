package io.sqm.core.traits;

import io.sqm.core.Join;
import io.sqm.core.Query;
import io.sqm.core.SelectQuery;
import io.sqm.core.views.Queries;

import java.util.List;

/**
 * An interface to get access to {@link SelectQuery#joins()} from the {@link Query} interface.
 * Use {@link Queries#joins(Query)} to get them.
 */
public interface HasJoins {
    List<Join> joins();
}
