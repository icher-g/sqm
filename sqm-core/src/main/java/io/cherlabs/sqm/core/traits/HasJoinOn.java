package io.cherlabs.sqm.core.traits;

import io.cherlabs.sqm.core.Filter;
import io.cherlabs.sqm.core.Join;
import io.cherlabs.sqm.core.TableJoin;

/**
 * An interface to get access to a {@link TableJoin#on()} on top of {@link io.cherlabs.sqm.core.Join} interface.
 * Use {@link io.cherlabs.sqm.core.views.Joins#on(Join)} to get it.
 */
public interface HasJoinOn {
    Filter on();
}
