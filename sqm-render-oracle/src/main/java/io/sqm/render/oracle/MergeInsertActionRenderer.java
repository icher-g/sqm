package io.sqm.render.oracle;

import io.sqm.core.MergeInsertAction;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders Oracle MERGE insert actions.
 */
public class MergeInsertActionRenderer extends io.sqm.render.ansi.MergeInsertActionRenderer {

    /**
     * Creates an Oracle merge-insert-action renderer.
     */
    public MergeInsertActionRenderer() {
    }

    @Override
    public void render(MergeInsertAction node, RenderContext ctx, SqlWriter w) {
        renderSupportedAction(node, ctx, w);
    }
}
