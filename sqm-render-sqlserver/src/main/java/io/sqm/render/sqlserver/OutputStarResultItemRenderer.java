package io.sqm.render.sqlserver;

import io.sqm.core.OutputRowSource;
import io.sqm.core.OutputStarResultItem;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders SQL Server {@code inserted.*} and {@code deleted.*} output items.
 */
public class OutputStarResultItemRenderer extends io.sqm.render.ansi.OutputStarResultItemRenderer {

    /**
     * Creates an output-star renderer.
     */
    public OutputStarResultItemRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(OutputStarResultItem node, RenderContext ctx, SqlWriter w) {
        w.append(node.source() == OutputRowSource.INSERTED ? "inserted" : "deleted")
            .append(".*");
    }
}
