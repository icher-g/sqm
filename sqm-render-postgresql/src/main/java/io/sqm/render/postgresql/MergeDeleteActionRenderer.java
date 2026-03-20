package io.sqm.render.postgresql;

import io.sqm.core.MergeDeleteAction;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders PostgreSQL MERGE delete actions.
 */
public class MergeDeleteActionRenderer extends io.sqm.render.ansi.MergeDeleteActionRenderer {

    /**
     * Creates a PostgreSQL merge-delete-action renderer.
     */
    public MergeDeleteActionRenderer() {
    }

    @Override
    public void render(MergeDeleteAction node, RenderContext ctx, SqlWriter w) {
        w.append("DELETE");
    }
}
