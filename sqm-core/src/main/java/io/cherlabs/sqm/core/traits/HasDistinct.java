package io.cherlabs.sqm.core.traits;

import io.cherlabs.sqm.core.Column;
import io.cherlabs.sqm.core.FunctionColumn;

/**
 * An interface to get the {@link FunctionColumn#distinct()} boolean from the {@link io.cherlabs.sqm.core.Column} interface.
 * Use {@link io.cherlabs.sqm.core.views.Columns#distinct(Column)} to get it.
 */
public interface HasDistinct {
    boolean distinct();
}
