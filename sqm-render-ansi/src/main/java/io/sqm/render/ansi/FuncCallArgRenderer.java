package io.sqm.render.ansi;

import io.sqm.core.FunctionExpr;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

public class FuncCallArgRenderer implements Renderer<FunctionExpr.Arg.Function> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(FunctionExpr.Arg.Function node, RenderContext ctx, SqlWriter w) {
        w.append(node.call());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<FunctionExpr.Arg.Function> targetType() {
        return FunctionExpr.Arg.Function.class;
    }
}
