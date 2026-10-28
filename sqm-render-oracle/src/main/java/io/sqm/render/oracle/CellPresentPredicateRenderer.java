package io.sqm.render.oracle;

import io.sqm.core.CellPresentPredicate;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders Oracle MODEL syntax such as {@code sales[2025] IS PRESENT} through registered child renderers.
 */
public class CellPresentPredicateRenderer extends io.sqm.render.ansi.CellPresentPredicateRenderer {
    /**
     * Creates an Oracle CellPresentPredicate renderer.
     */
    public CellPresentPredicateRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(CellPresentPredicate node, RenderContext ctx, SqlWriter w) {
        ModelRenderSupport.require(ctx);
        w.append(node.cell()).append(" IS PRESENT");
    }
}
