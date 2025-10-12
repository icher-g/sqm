package io.cherlabs.sqm.core.traits;

import io.cherlabs.sqm.core.Column;
import io.cherlabs.sqm.core.Query;
import io.cherlabs.sqm.core.QueryColumn;

/**
 * An interface to get access to {@link QueryColumn#query()} on top of {@link io.cherlabs.sqm.core.Column} interface.
 * Use {@link io.cherlabs.sqm.core.views.Columns#query(Column)} to get it.
 */
public interface HasQuery {
    Query query();
}
