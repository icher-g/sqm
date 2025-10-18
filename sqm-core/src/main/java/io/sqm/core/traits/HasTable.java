package io.sqm.core.traits;

import io.sqm.core.*;
import io.sqm.core.views.Joins;
import io.sqm.core.views.Queries;

/**
 * An interface to get access to {@link TableJoin#table()} on top of {@link Join} interface or
 * to get access to {@link SelectQuery#table()} on top of {@link Query} interface.
 * Use {@link Joins#table(Join)} to get it.
 * Use {@link Queries#table(Query)} to get it.
 */
public interface HasTable {
    Table table();
}
