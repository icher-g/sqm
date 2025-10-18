package io.sqm.core.traits;

import io.sqm.core.Column;
import io.sqm.core.Query;
import io.sqm.core.QueryColumn;
import io.sqm.core.views.Columns;

/**
 * An interface to get access to {@link QueryColumn#query()} on top of {@link Column} interface.
 * Use {@link Columns#query(Column)} to get it.
 */
public interface HasQuery {
    Query query();
}
