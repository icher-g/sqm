package io.sqm.render.ansi;

import io.sqm.core.FunctionExpr;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

public class FuncStarArgRenderer implements Renderer<FunctionExpr.Arg.Star> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(FunctionExpr.Arg.Star node, RenderContext ctx, SqlWriter w) {
        w.append("*");
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<FunctionExpr.Arg.Star> targetType() {
        return FunctionExpr.Arg.Star.class;
    }
}
