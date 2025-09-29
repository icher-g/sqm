package io.cherlabs.sqlmodel.core.traits;

import io.cherlabs.sqlmodel.core.Column;
import io.cherlabs.sqlmodel.core.Filter;
import io.cherlabs.sqlmodel.core.Join;

/**
 * An interface to access expr from classes such as
 * {@link io.cherlabs.sqlmodel.core.ExpressionColumn},
 * {@link io.cherlabs.sqlmodel.core.ExpressionFilter},
 * {@link io.cherlabs.sqlmodel.core.ExpressionJoin}.
 * Use {@link io.cherlabs.sqlmodel.core.views.Columns#expr(Column)}
 * {@link io.cherlabs.sqlmodel.core.views.Filters#expr(Filter)}
 * {@link io.cherlabs.sqlmodel.core.views.Joins#expr(Join)} respectively.
 */
public interface HasExpr {
    String expr();
}
