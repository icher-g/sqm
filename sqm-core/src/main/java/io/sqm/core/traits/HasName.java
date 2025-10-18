package io.sqm.core.traits;

import io.sqm.core.Column;
import io.sqm.core.Table;
import io.sqm.core.views.Columns;
import io.sqm.core.views.Tables;

/**
 * An interface to get the name of a {@link Column} or a {@link Table}.
 * Use {@link Columns#name(Column)} or
 * {@link Tables#name(Table)} respectively.
 */
public interface HasName {
    String name();
}
