package io.sqm.render.postgresql;

import io.sqm.core.MergeDoNothingAction;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders PostgreSQL MERGE do-nothing actions.
 */
public class MergeDoNothingActionRenderer extends io.sqm.render.ansi.MergeDoNothingActionRenderer {

    /**
     * Creates a PostgreSQL merge-do-nothing-action renderer.
     */
    public MergeDoNothingActionRenderer() {
    }

    @Override
    public void render(MergeDoNothingAction node, RenderContext ctx, SqlWriter w) {
        w.append("DO NOTHING");
    }
}
