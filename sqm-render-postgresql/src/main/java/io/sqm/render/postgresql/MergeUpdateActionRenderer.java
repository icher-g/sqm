package io.sqm.render.postgresql;

import io.sqm.core.MergeUpdateAction;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders PostgreSQL MERGE update actions.
 */
public class MergeUpdateActionRenderer extends io.sqm.render.ansi.MergeUpdateActionRenderer {

    /**
     * Creates a PostgreSQL merge-update-action renderer.
     */
    public MergeUpdateActionRenderer() {
    }

    @Override
    public void render(MergeUpdateAction node, RenderContext ctx, SqlWriter w) {
        w.append("UPDATE SET").space();
        w.comma(node.assignments());
    }
}
