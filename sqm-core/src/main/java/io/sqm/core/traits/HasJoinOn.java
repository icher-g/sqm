package io.sqm.core.traits;

import io.sqm.core.Filter;
import io.sqm.core.Join;
import io.sqm.core.TableJoin;
import io.sqm.core.views.Joins;

/**
 * An interface to get access to a {@link TableJoin#on()} on top of {@link Join} interface.
 * Use {@link Joins#on(Join)} to get it.
 */
public interface HasJoinOn {
    Filter on();
}
