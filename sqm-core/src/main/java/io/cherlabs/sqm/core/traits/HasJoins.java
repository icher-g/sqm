package io.cherlabs.sqm.core.traits;

import io.cherlabs.sqm.core.Join;
import io.cherlabs.sqm.core.Query;
import io.cherlabs.sqm.core.SelectQuery;

import java.util.List;

/**
 * An interface to get access to {@link SelectQuery#joins()} from the {@link io.cherlabs.sqm.core.Query} interface.
 * Use {@link io.cherlabs.sqm.core.views.Queries#joins(Query)} to get them.
 */
public interface HasJoins {
    List<Join> joins();
}
