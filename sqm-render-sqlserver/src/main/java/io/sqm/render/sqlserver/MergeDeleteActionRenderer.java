package io.sqm.render.sqlserver;

import io.sqm.core.MergeDeleteAction;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders SQL Server MERGE delete actions.
 */
public class MergeDeleteActionRenderer extends io.sqm.render.ansi.MergeDeleteActionRenderer {

    /**
     * Creates a SQL Server merge-delete-action renderer.
     */
    public MergeDeleteActionRenderer() {
    }

    @Override
    public void render(MergeDeleteAction node, RenderContext ctx, SqlWriter w) {
        w.append("DELETE");
    }
}
