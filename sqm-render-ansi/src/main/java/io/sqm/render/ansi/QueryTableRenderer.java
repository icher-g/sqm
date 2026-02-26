package io.sqm.render.ansi;

import io.sqm.core.QueryTable;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders derived query tables.
 */
public class QueryTableRenderer implements Renderer<QueryTable> {
    /**
     * Creates a query-table renderer.
     */
    public QueryTableRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(QueryTable node, RenderContext ctx, SqlWriter w) {
        w.append(node.query(), true, true);
        renderAliased(node, ctx, w);
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<QueryTable> targetType() {
        return QueryTable.class;
    }
}
