package io.sqm.render.sqlserver;

import io.sqm.core.MergeInsertAction;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders SQL Server MERGE insert actions.
 */
public class MergeInsertActionRenderer extends io.sqm.render.ansi.MergeInsertActionRenderer {

    /**
     * Creates a SQL Server merge-insert-action renderer.
     */
    public MergeInsertActionRenderer() {
    }

    @Override
    public void render(MergeInsertAction node, RenderContext ctx, SqlWriter w) {
        renderSupportedAction(node, ctx, w);
    }
}
