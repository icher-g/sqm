package io.sqm.render.oracle;

import io.sqm.core.CellTarget;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders basic Oracle MODEL CellTarget syntax through registered child renderers.
 */
public class CellTargetRenderer extends io.sqm.render.ansi.CellTargetRenderer {
    /**
     * Creates an Oracle CellTarget renderer.
     */
    public CellTargetRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(CellTarget node, RenderContext ctx, SqlWriter w) {
        ModelRenderSupport.require(ctx);
        w.append(renderIdentifier(node.measure(), ctx.dialect().quoter())).append("[");
        w.comma(node.addresses()).append("]");
    }
}
