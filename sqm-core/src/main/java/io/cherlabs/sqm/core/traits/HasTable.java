package io.cherlabs.sqm.core.traits;

import io.cherlabs.sqm.core.Join;
import io.cherlabs.sqm.core.Table;
import io.cherlabs.sqm.core.TableJoin;

/**
 * An interface to get a {@link TableJoin#table()} on top of {@link io.cherlabs.sqm.core.Join} interface.
 * Use {@link io.cherlabs.sqm.core.views.Joins#table(Join)} to get it.
 */
public interface HasTable {
    Table table();
}
