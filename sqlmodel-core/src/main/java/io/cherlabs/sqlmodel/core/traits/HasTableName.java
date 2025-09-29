package io.cherlabs.sqlmodel.core.traits;

import io.cherlabs.sqlmodel.core.Column;
import io.cherlabs.sqlmodel.core.NamedColumn;

/**
 * An interface to get a {@link NamedColumn#table()} on top of {@link io.cherlabs.sqlmodel.core.Column} interface.
 * Use {@link io.cherlabs.sqlmodel.core.views.Columns#table(Column)} to get it.
 */
public interface HasTableName {
    String table();
}
