package io.sqm.core.traits;

import io.sqm.core.Join;
import io.sqm.core.TableJoin;
import io.sqm.core.views.Joins;

/**
 * An interface to get access to a {@link TableJoin#joinType()} on top of {@link Join} interface.
 * Use {@link Joins#joinType(Join)} to get it.
 */
public interface HasJoinType {
    Join.JoinType joinType();
}
