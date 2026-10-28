package io.sqm.render.oracle;

import io.sqm.core.PreviousModelValueExpr;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders Oracle MODEL syntax such as {@code PREVIOUS(sales[2025])} through registered child renderers.
 */
public class PreviousModelValueExprRenderer extends io.sqm.render.ansi.PreviousModelValueExprRenderer {
    /**
     * Creates an Oracle PreviousModelValueExpr renderer.
     */
    public PreviousModelValueExprRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(PreviousModelValueExpr node, RenderContext ctx, SqlWriter w) {
        ModelRenderSupport.require(ctx);
        w.append("PREVIOUS(").append(node.cell()).append(")");
    }
}
