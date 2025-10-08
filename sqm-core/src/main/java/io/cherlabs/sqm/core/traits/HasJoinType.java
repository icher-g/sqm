package io.cherlabs.sqm.core.traits;

import io.cherlabs.sqm.core.Join;
import io.cherlabs.sqm.core.TableJoin;

/**
 * An interface to get access to a {@link TableJoin#joinType()} on top of {@link Join} interface.
 * Use {@link io.cherlabs.sqm.core.views.Joins#joinType(Join)} to get it.
 */
public interface HasJoinType {
    Join.JoinType joinType();
}
