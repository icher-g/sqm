package io.sqm.render.oracle;

import io.sqm.core.CellRefExpr;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders basic Oracle MODEL CellRefExpr syntax through registered child renderers.
 */
public class CellRefExprRenderer extends io.sqm.render.ansi.CellRefExprRenderer {
    /**
     * Creates an Oracle CellRefExpr renderer.
     */
    public CellRefExprRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(CellRefExpr node, RenderContext ctx, SqlWriter w) {
        ModelRenderSupport.require(ctx);
        if (node.model() != null) w.append(renderIdentifier(node.model(), ctx.dialect().quoter())).append(".");
        w.append(renderIdentifier(node.measure(), ctx.dialect().quoter())).append("[");
        w.comma(node.selectors()).append("]");
    }
}
