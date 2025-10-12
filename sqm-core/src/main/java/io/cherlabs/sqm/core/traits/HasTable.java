package io.cherlabs.sqm.core.traits;

import io.cherlabs.sqm.core.*;

/**
 * An interface to get access to {@link TableJoin#table()} on top of {@link io.cherlabs.sqm.core.Join} interface or
 * to get access to {@link SelectQuery#table()} on top of {@link io.cherlabs.sqm.core.Query} interface.
 * Use {@link io.cherlabs.sqm.core.views.Joins#table(Join)} to get it.
 * Use {@link io.cherlabs.sqm.core.views.Queries#table(Query)} to get it.
 */
public interface HasTable {
    Table table();
}
