package io.cherlabs.sqm.core.traits;

import io.cherlabs.sqm.core.NamedTable;
import io.cherlabs.sqm.core.Table;

/**
 * An interface to get a schema from the {@link NamedTable#schema()} on top of {@link io.cherlabs.sqm.core.Table} interface.
 * Use {@link io.cherlabs.sqm.core.views.Tables#schema(Table)} to get it.
 */
public interface HasSchema {
    String schema();
}
