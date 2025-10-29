package io.sqm.render.ansi;

import io.sqm.core.LiteralExpr;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

public class LiteralExprRenderer implements Renderer<LiteralExpr> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(LiteralExpr node, RenderContext ctx, SqlWriter w) {
        w.append(ctx.bindOrFormat(node.value()));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<LiteralExpr> targetType() {
        return LiteralExpr.class;
    }
}
