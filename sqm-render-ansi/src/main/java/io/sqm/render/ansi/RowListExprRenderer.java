package io.sqm.render.ansi;

import io.sqm.core.RowListExpr;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders row-list expressions.
 */
public class RowListExprRenderer implements Renderer<RowListExpr> {
    /**
     * Creates a row-list-expression renderer.
     */
    public RowListExprRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(RowListExpr node, RenderContext ctx, SqlWriter w) {
        w.comma(node.rows(), true);
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<RowListExpr> targetType() {
        return RowListExpr.class;
    }
}
