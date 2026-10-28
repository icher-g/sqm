package io.sqm.render.oracle;

import io.sqm.core.CurrentDimensionExpr;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders Oracle MODEL syntax such as {@code CV(year_key)} through registered child renderers.
 */
public class CurrentDimensionExprRenderer extends io.sqm.render.ansi.CurrentDimensionExprRenderer {
    /**
     * Creates an Oracle CurrentDimensionExpr renderer.
     */
    public CurrentDimensionExprRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(CurrentDimensionExpr node, RenderContext ctx, SqlWriter w) {
        ModelRenderSupport.require(ctx);
        w.append("CV(");
        if (node.dimension() != null) w.append(renderIdentifier(node.dimension(), ctx.dialect().quoter()));
        w.append(")");
    }
}
