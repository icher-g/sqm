package io.sqm.render.ansi;

import io.sqm.core.FunctionExpr;
import io.sqm.core.OverSpec;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

public class FunctionExprRenderer implements Renderer<FunctionExpr> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(FunctionExpr node, RenderContext ctx, SqlWriter w) {
        // Render qualified function name with quote preservation/fallback per part.
        var quoter = ctx.dialect().quoter();
        var parts = node.name().parts();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) w.append(".");
            w.append(renderIdentifier(parts.get(i), quoter));
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

        if (node.withinGroup() != null) {
            w.space().append("WITHIN GROUP").space().append("(");
            w.append(node.withinGroup());
            w.append(")");
        }

        if (node.filter() != null) {
            w.space().append("FILTER").space().append("(");
            w.append("WHERE").space();
            w.append(node.filter());
            w.append(")");
        }

        if (node.over() != null) {
            w.space().append("OVER").space();
            w.append(node.over(), node.over() instanceof OverSpec.Def, false);
        }
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
