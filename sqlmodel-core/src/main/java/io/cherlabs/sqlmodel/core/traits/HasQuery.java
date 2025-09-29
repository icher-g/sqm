package io.cherlabs.sqlmodel.core.traits;

import io.cherlabs.sqlmodel.core.Column;
import io.cherlabs.sqlmodel.core.Query;
import io.cherlabs.sqlmodel.core.QueryColumn;

/**
 * An interface to get access to {@link QueryColumn#query()} on top of {@link io.cherlabs.sqlmodel.core.Column} interface.
 * Use {@link io.cherlabs.sqlmodel.core.views.Columns#query(Column)} to get it.
 */
public interface HasQuery {
    Query<?> query();
}
