package io.sqm.render.ansi;

import io.sqm.core.NamedParamExpr;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

public class NamedParamExprRenderer implements Renderer<NamedParamExpr> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(NamedParamExpr node, RenderContext ctx, SqlWriter w) {
        // ANSI does not support named parameters so we use the default marker.
        w.append("?");
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<NamedParamExpr> targetType() {
        return NamedParamExpr.class;
    }
}
