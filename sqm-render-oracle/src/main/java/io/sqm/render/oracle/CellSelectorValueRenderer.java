package io.sqm.render.oracle;

import io.sqm.core.CellSelector;
import io.sqm.core.Predicate;
import io.sqm.core.oracle.internal.ModelSyntaxChecker;
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
        ModelSyntaxChecker.scalar(node.value());
        w.append(node.value());
    }
}
