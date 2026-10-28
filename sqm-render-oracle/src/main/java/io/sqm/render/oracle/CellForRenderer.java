package io.sqm.render.oracle;

import io.sqm.core.CellFor;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders Oracle MODEL syntax such as {@code FOR year_key IN (2025, 2026)} through registered child renderers.
 */
public class CellForRenderer extends io.sqm.render.ansi.CellForRenderer {
    /**
     * Creates an Oracle CellFor renderer.
     */
    public CellForRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(CellFor node, RenderContext ctx, SqlWriter w) {
        ModelRenderSupport.require(ctx);
        switch (node) {
            case CellFor.Values values -> w.append(CellFor.Values.class, values);
            case CellFor.Range range -> w.append(CellFor.Range.class, range);
        }
    }
}
