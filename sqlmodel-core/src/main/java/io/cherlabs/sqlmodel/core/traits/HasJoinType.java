package io.cherlabs.sqlmodel.core.traits;

import io.cherlabs.sqlmodel.core.Join;
import io.cherlabs.sqlmodel.core.TableJoin;

/**
 * An interface to get access to a {@link TableJoin#joinType()} on top of {@link Join} interface.
 * Use {@link io.cherlabs.sqlmodel.core.views.Joins#joinType(Join)} to get it.
 */
public interface HasJoinType {
    Join.JoinType joinType();
}
