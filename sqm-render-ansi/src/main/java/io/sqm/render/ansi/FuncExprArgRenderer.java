package io.sqm.render.ansi;

import io.sqm.core.FunctionExpr;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders expression arguments in function calls.
 */
public class FuncExprArgRenderer implements Renderer<FunctionExpr.Arg.ExprArg> {
    /**
     * Creates a function-expression-argument renderer.
     */
    public FuncExprArgRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(FunctionExpr.Arg.ExprArg node, RenderContext ctx, SqlWriter w) {
        w.append(node.expr());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<FunctionExpr.Arg.ExprArg> targetType() {
        return FunctionExpr.Arg.ExprArg.class;
    }
}
