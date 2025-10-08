package io.cherlabs.sqm.core.traits;

import io.cherlabs.sqm.core.Column;
import io.cherlabs.sqm.core.Table;

/**
 * An interface to get the name of a {@link io.cherlabs.sqm.core.Column} or a {@link io.cherlabs.sqm.core.Table}.
 * Use {@link io.cherlabs.sqm.core.views.Columns#name(Column)} or
 * {@link io.cherlabs.sqm.core.views.Tables#name(Table)} respectively.
 */
public interface HasName {
    String name();
}
