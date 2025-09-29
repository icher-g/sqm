package io.cherlabs.sqlmodel.core.traits;

import io.cherlabs.sqlmodel.core.Column;
import io.cherlabs.sqlmodel.core.FunctionColumn;

/**
 * An interface to get the {@link FunctionColumn#distinct()} boolean from the {@link io.cherlabs.sqlmodel.core.Column} interface.
 * Use {@link io.cherlabs.sqlmodel.core.views.Columns#distinct(Column)} to get it.
 */
public interface HasDistinct {
    boolean distinct();
}
