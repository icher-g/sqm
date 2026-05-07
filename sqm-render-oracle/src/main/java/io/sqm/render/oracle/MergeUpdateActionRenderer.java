package io.sqm.render.oracle;

import io.sqm.core.MergeUpdateAction;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders Oracle MERGE update actions.
 */
public class MergeUpdateActionRenderer extends io.sqm.render.ansi.MergeUpdateActionRenderer {

    /**
     * Creates an Oracle merge-update-action renderer.
     */
    public MergeUpdateActionRenderer() {
    }

    @Override
    public void render(MergeUpdateAction node, RenderContext ctx, SqlWriter w) {
        renderSupportedAction(node, w);
    }
}
