package io.sqm.core.traits;

import io.sqm.core.Column;
import io.sqm.core.Filter;
import io.sqm.core.Join;
import io.sqm.core.ExpressionColumn;
import io.sqm.core.ExpressionFilter;
import io.sqm.core.ExpressionJoin;
import io.sqm.core.views.Columns;
import io.sqm.core.views.Filters;
import io.sqm.core.views.Joins;

/**
 * An interface to access expr from classes such as
 * {@link ExpressionColumn},
 * {@link ExpressionFilter},
 * {@link ExpressionJoin}.
 * Use {@link Columns#expr(Column)}
 * {@link Filters#expr(Filter)}
 * {@link Joins#expr(Join)} respectively.
 */
public interface HasExpr {
    String expr();
}
