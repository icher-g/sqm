package io.sqm.render.oracle;

import io.sqm.core.IterationNumberExpr;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders Oracle MODEL syntax such as {@code ITERATION_NUMBER} through registered child renderers.
 */
public class IterationNumberExprRenderer extends io.sqm.render.ansi.IterationNumberExprRenderer {
    /**
     * Creates an Oracle IterationNumberExpr renderer.
     */
    public IterationNumberExprRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(IterationNumberExpr node, RenderContext ctx, SqlWriter w) {
        ModelRenderSupport.require(ctx);
        w.append("ITERATION_NUMBER");
    }
}
