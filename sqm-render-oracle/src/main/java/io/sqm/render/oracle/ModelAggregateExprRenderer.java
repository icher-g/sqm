package io.sqm.render.oracle;

import io.sqm.core.ModelAggregateExpr;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders Oracle MODEL syntax such as {@code SUM(sales)[ANY, year_key BETWEEN 2025 AND 2027]} through registered child renderers.
 */
public class ModelAggregateExprRenderer extends io.sqm.render.ansi.ModelAggregateExprRenderer {
    /**
     * Creates an Oracle ModelAggregateExpr renderer.
     */
    public ModelAggregateExprRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(ModelAggregateExpr node, RenderContext ctx, SqlWriter w) {
        ModelRenderSupport.require(ctx);
        w.append(node.aggregate()).append("[").comma(node.selectors()).append("]");
    }
}
