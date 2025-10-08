package io.cherlabs.sqm.core.traits;

import io.cherlabs.sqm.core.Column;
import io.cherlabs.sqm.core.NamedColumn;

/**
 * An interface to get a {@link NamedColumn#table()} on top of {@link io.cherlabs.sqm.core.Column} interface.
 * Use {@link io.cherlabs.sqm.core.views.Columns#table(Column)} to get it.
 */
public interface HasTableName {
    String table();
}
