package io.sqm.render.oracle;

import io.sqm.core.CellSelector;
import io.sqm.core.Predicate;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders basic Oracle MODEL CellSelector.Value syntax through registered child renderers.
 */
public class CellSelectorValueRenderer extends io.sqm.render.ansi.CellSelectorValueRenderer {
    /**
     * Creates an Oracle CellSelector.Value renderer.
     */
    public CellSelectorValueRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(CellSelector.Value node, RenderContext ctx, SqlWriter w) {
        ModelRenderSupport.require(ctx);
        if (node.value() instanceof Predicate) throw ModelRenderSupport.unsupported("predicate-valued selectors", ctx);
        w.append(node.value());
    }
}
