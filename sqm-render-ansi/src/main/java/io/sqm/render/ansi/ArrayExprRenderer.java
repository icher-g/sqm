package io.sqm.render.ansi;

import io.sqm.core.ArrayExpr;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

public class ArrayExprRenderer implements Renderer<ArrayExpr> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(ArrayExpr node, RenderContext ctx, SqlWriter w) {
        throw new UnsupportedOperationException(
            "Array expressions are not supported by ANSI SQL renderer"
        );
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<? extends ArrayExpr> targetType() {
        return ArrayExpr.class;
    }
}
