package io.sqm.render.ansi;

import io.sqm.core.ConcatExpr;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders concatenation expressions using {@code ||}.
 */
public class ConcatExprRenderer implements Renderer<ConcatExpr> {

    /**
     * Creates a concatenation-expression renderer.
     */
    public ConcatExprRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(ConcatExpr node, RenderContext ctx, SqlWriter w) {
        boolean first = true;
        for (var arg : node.args()) {
            if (!first) {
                w.space().append("||").space();
            }
            w.append(arg);
            first = false;
        }
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<? extends ConcatExpr> targetType() {
        return ConcatExpr.class;
    }
}
