package io.sqm.render.postgresql;

import io.sqm.core.FunctionExpr;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * PostgreSQL-specific function expression renderer.
 *
 * <p>This renderer supports aggregate input ordering inside the function
 * argument list, for example {@code ARRAY_AGG(x ORDER BY x)}.</p>
 */
public class FunctionExprRenderer extends io.sqm.render.ansi.FunctionExprRenderer {
    /**
     * Creates a PostgreSQL function expression renderer.
     */
    public FunctionExprRenderer() {
    }

    /**
     * Renders PostgreSQL aggregate input ordering inside function parentheses.
     *
     * @param node function expression to render.
     * @param ctx render context.
     * @param w SQL writer.
     */
    @Override
    protected void renderOrderBy(FunctionExpr node, RenderContext ctx, SqlWriter w) {
        if (node.orderBy() != null) {
            w.space().append(node.orderBy());
        }
    }
}
