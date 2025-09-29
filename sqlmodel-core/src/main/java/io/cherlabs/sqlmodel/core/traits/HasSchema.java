package io.cherlabs.sqlmodel.core.traits;

import io.cherlabs.sqlmodel.core.NamedTable;
import io.cherlabs.sqlmodel.core.Table;

/**
 * An interface to get a schema from the {@link NamedTable#schema()} on top of {@link io.cherlabs.sqlmodel.core.Table} interface.
 * Use {@link io.cherlabs.sqlmodel.core.views.Tables#schema(Table)} to get it.
 */
public interface HasSchema {
    String schema();
}
