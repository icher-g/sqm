package io.cherlabs.sqlmodel.core.traits;

import io.cherlabs.sqlmodel.core.Filter;
import io.cherlabs.sqlmodel.core.Join;
import io.cherlabs.sqlmodel.core.TableJoin;

/**
 * An interface to get access to a {@link TableJoin#on()} on top of {@link io.cherlabs.sqlmodel.core.Join} interface.
 * Use {@link io.cherlabs.sqlmodel.core.views.Joins#on(Join)} to get it.
 */
public interface HasJoinOn {
    Filter on();
}
