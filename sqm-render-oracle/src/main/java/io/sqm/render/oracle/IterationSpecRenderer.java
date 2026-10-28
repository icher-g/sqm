package io.sqm.render.oracle;

import io.sqm.core.IterationSpec;
import io.sqm.core.oracle.internal.ModelSyntaxChecker;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders Oracle MODEL syntax such as {@code ITERATE (100) UNTIL (sales[1] < 1)} through registered child renderers.
 */
public class IterationSpecRenderer extends io.sqm.render.ansi.IterationSpecRenderer {
    /**
     * Creates an Oracle IterationSpec renderer.
     */
    public IterationSpecRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(IterationSpec node, RenderContext ctx, SqlWriter w) {
        ModelRenderSupport.require(ctx);
        ModelSyntaxChecker.iteration(node);
        w.append("ITERATE (").append(node.limit()).append(")");
        if (node.until() != null) w.append(" UNTIL (").append(node.until()).append(")");
    }
}
