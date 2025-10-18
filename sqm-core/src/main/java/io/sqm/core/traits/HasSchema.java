package io.sqm.core.traits;

import io.sqm.core.NamedTable;
import io.sqm.core.Table;
import io.sqm.core.views.Tables;

/**
 * An interface to get a schema from the {@link NamedTable#schema()} on top of {@link Table} interface.
 * Use {@link Tables#schema(Table)} to get it.
 */
public interface HasSchema {
    String schema();
}
