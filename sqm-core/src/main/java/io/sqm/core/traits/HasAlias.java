package io.sqm.core.traits;

import io.sqm.core.Column;
import io.sqm.core.Table;
import io.sqm.core.views.Columns;

/**
 * An interface to access alias on {@link Column}, {@link Table} etc.
 * Use {@link Columns#alias(Column)} to get alias.
 */
public interface HasAlias {
    String alias();
}
