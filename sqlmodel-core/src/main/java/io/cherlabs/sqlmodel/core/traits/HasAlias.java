package io.cherlabs.sqlmodel.core.traits;

import io.cherlabs.sqlmodel.core.Column;

/**
 * An interface to access alias on {@link io.cherlabs.sqlmodel.core.Column}, {@link io.cherlabs.sqlmodel.core.Table} etc.
 * Use {@link io.cherlabs.sqlmodel.core.views.Columns#alias(Column)} to get alias.
 */
public interface HasAlias {
    String alias();
}
