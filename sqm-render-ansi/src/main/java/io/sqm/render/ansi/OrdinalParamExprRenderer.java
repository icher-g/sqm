package io.sqm.render.ansi;

import io.sqm.core.OrdinalParamExpr;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders ordinal parameter expressions.
 */
public class OrdinalParamExprRenderer implements Renderer<OrdinalParamExpr> {
    /**
     * Creates an ordinal-parameter renderer.
     */
    public OrdinalParamExprRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(OrdinalParamExpr node, RenderContext ctx, SqlWriter w) {
        // ANSI does not support named parameters so we use the default marker.
        w.append("?");
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<OrdinalParamExpr> targetType() {
        return OrdinalParamExpr.class;
    }
}
