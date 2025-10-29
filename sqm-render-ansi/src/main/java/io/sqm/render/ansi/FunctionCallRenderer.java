package io.sqm.render.ansi;

import io.sqm.core.FunctionExpr;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

public class FunctionCallRenderer implements Renderer<FunctionExpr> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(FunctionExpr node, RenderContext ctx, SqlWriter w) {
        // Render qualified function name with identifier quoting per dialect
        var quoter = ctx.dialect().quoter();
        var parts = node.name().split("\\.");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) w.append(".");
            w.append(quoter.quoteIfNeeded(parts[i]));
        }

        w.append("(");

        if (node.distinctArg() != null && node.distinctArg()) {
            w.append("DISTINCT");
            // add space if there will be args after DISTINCT
            if (!node.args().isEmpty()) w.space();
        }

        // Render arguments list
        w.comma(node.args());
        w.append(")");
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<FunctionExpr> targetType() {
        return FunctionExpr.class;
    }
}
