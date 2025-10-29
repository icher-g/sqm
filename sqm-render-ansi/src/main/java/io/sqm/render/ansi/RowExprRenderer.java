package io.sqm.render.ansi;

import io.sqm.core.RowExpr;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

public class RowExprRenderer implements Renderer<RowExpr> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(RowExpr node, RenderContext ctx, SqlWriter w) {
        w.comma(node.items());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<RowExpr> targetType() {
        return RowExpr.class;
    }
}
