package io.cherlabs.sqm.core.traits;

import io.cherlabs.sqm.core.Column;

/**
 * An interface to access alias on {@link io.cherlabs.sqm.core.Column}, {@link io.cherlabs.sqm.core.Table} etc.
 * Use {@link io.cherlabs.sqm.core.views.Columns#alias(Column)} to get alias.
 */
public interface HasAlias {
    String alias();
}
