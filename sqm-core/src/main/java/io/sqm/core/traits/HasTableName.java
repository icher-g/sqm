package io.sqm.core.traits;

import io.sqm.core.Column;
import io.sqm.core.NamedColumn;
import io.sqm.core.views.Columns;

/**
 * An interface to get a {@link NamedColumn#table()} on top of {@link Column} interface.
 * Use {@link Columns#table(Column)} to get it.
 */
public interface HasTableName {
    String table();
}
