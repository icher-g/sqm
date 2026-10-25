package io.sqm.render.oracle;

import io.sqm.core.CellAddress;
import io.sqm.core.CellFor;
import io.sqm.core.CellSelector;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders basic Oracle MODEL CellAddress syntax through registered child renderers.
 */
public class CellAddressRenderer extends io.sqm.render.ansi.CellAddressRenderer {
    /**
     * Creates an Oracle CellAddress renderer.
     */
    public CellAddressRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(CellAddress node, RenderContext ctx, SqlWriter w) {
        ModelRenderSupport.require(ctx);
        if (node instanceof CellSelector selector) w.append(CellSelector.class, selector);
        else w.append(CellFor.class, (CellFor) node);
    }
}
