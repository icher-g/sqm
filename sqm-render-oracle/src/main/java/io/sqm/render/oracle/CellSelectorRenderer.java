package io.sqm.render.oracle;

import io.sqm.core.CellSelector;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders basic Oracle MODEL CellSelector syntax through registered child renderers.
 */
public class CellSelectorRenderer extends io.sqm.render.ansi.CellSelectorRenderer {
    /**
     * Creates an Oracle CellSelector renderer.
     */
    public CellSelectorRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(CellSelector node, RenderContext ctx, SqlWriter w) {
        ModelRenderSupport.require(ctx);
        if (node instanceof CellSelector.Value value) w.append(CellSelector.Value.class, value);
        else w.append(CellSelector.Condition.class, (CellSelector.Condition) node);
    }
}
