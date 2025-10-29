package io.sqm.render.ansi;

import io.sqm.core.QueryTable;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

public class QueryTableRenderer implements Renderer<QueryTable> {
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

        var alias = node.alias();
        if (alias != null && !alias.isBlank()) {
            var quoter = ctx.dialect().quoter();
            w.space().append("AS").space().append(quoter.quoteIfNeeded(alias));
        }
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
