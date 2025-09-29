package io.cherlabs.sqlmodel.core.traits;

import io.cherlabs.sqlmodel.core.Join;
import io.cherlabs.sqlmodel.core.Table;
import io.cherlabs.sqlmodel.core.TableJoin;

/**
 * An interface to get a {@link TableJoin#table()} on top of {@link io.cherlabs.sqlmodel.core.Join} interface.
 * Use {@link io.cherlabs.sqlmodel.core.views.Joins#table(Join)} to get it.
 */
public interface HasTable {
    Table table();
}
