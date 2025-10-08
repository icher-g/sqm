package io.cherlabs.sqm.core.traits;

import io.cherlabs.sqm.core.Column;
import io.cherlabs.sqm.core.Filter;
import io.cherlabs.sqm.core.Join;

/**
 * An interface to access expr from classes such as
 * {@link io.cherlabs.sqm.core.ExpressionColumn},
 * {@link io.cherlabs.sqm.core.ExpressionFilter},
 * {@link io.cherlabs.sqm.core.ExpressionJoin}.
 * Use {@link io.cherlabs.sqm.core.views.Columns#expr(Column)}
 * {@link io.cherlabs.sqm.core.views.Filters#expr(Filter)}
 * {@link io.cherlabs.sqm.core.views.Joins#expr(Join)} respectively.
 */
public interface HasExpr {
    String expr();
}
