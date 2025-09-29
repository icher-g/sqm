package io.cherlabs.sqlmodel.core.traits;

import io.cherlabs.sqlmodel.core.Column;
import io.cherlabs.sqlmodel.core.Table;

/**
 * An interface to get the name of a {@link io.cherlabs.sqlmodel.core.Column} or a {@link io.cherlabs.sqlmodel.core.Table}.
 * Use {@link io.cherlabs.sqlmodel.core.views.Columns#name(Column)} or
 * {@link io.cherlabs.sqlmodel.core.views.Tables#name(Table)} respectively.
 */
public interface HasName {
    String name();
}
