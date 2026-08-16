package io.sqm.render.oracle;

import io.sqm.core.SelectQuery;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders Oracle {@code SELECT} statements.
 */
public class SelectQueryRenderer extends io.sqm.render.ansi.SelectQueryRenderer {
    /**
     * Creates an Oracle SELECT-query renderer.
     */
    public SelectQueryRenderer() {
    }

    @Override
    protected void renderAfterSelectKeyword(SelectQuery node, RenderContext ctx, SqlWriter w) {
        OracleHintRenderSupport.renderStatementHints(node.hints(), "SELECT optimizer hints", ctx, w);
    }
}
