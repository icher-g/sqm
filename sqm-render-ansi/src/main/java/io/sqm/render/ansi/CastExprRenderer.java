package io.sqm.render.ansi;

import io.sqm.core.CastExpr;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders CAST expressions.
 */
public class CastExprRenderer implements Renderer<CastExpr> {
    /**
     * Creates a cast-expression renderer.
     */
    public CastExprRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(CastExpr node, RenderContext ctx, SqlWriter w) {
        w.append("CAST")
            .append("(")
            .append(node.expr()).space()
            .append("AS").space()
            .append(node.type())
            .append(")");
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<? extends CastExpr> targetType() {
        return CastExpr.class;
    }
}
