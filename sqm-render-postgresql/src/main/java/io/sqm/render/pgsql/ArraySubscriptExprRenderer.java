package io.sqm.render.pgsql;

import io.sqm.core.ArraySubscriptExpr;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

public class ArraySubscriptExprRenderer implements Renderer<ArraySubscriptExpr> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(ArraySubscriptExpr node, RenderContext ctx, SqlWriter w) {
        w.append(node.base()).append("[").append(node.index()).append("]");
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<? extends ArraySubscriptExpr> targetType() {
        return ArraySubscriptExpr.class;
    }
}
