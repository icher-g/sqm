package io.sqm.render.oracle;

import io.sqm.core.CellSelector;
import io.sqm.core.oracle.internal.ModelSyntaxChecker;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders Oracle MODEL syntax such as {@code year_key BETWEEN 2025 AND 2027} through registered child renderers.
 */
public class CellSelectorConditionRenderer extends io.sqm.render.ansi.CellSelectorConditionRenderer {
    /**
     * Creates an Oracle CellSelector.Condition renderer.
     */
    public CellSelectorConditionRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(CellSelector.Condition node, RenderContext ctx, SqlWriter w) {
        ModelRenderSupport.require(ctx);
        ModelSyntaxChecker.selector(node.predicate());
        w.append(node.predicate());
    }
}
