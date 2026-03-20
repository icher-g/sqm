package io.sqm.render.sqlserver;

import io.sqm.core.MergeDoNothingAction;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders SQL Server MERGE do-nothing actions as explicit unsupported syntax.
 */
public class MergeDoNothingActionRenderer extends io.sqm.render.ansi.MergeDoNothingActionRenderer {

    /**
     * Creates a SQL Server merge-do-nothing-action renderer.
     */
    public MergeDoNothingActionRenderer() {
    }

    @Override
    public void render(MergeDoNothingAction node, RenderContext ctx, SqlWriter w) {
        throw new UnsupportedOperationException("SQL Server MERGE DO NOTHING actions are not supported");
    }
}
