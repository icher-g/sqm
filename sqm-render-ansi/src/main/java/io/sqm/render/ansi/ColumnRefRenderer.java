package io.sqm.render.ansi;

import io.sqm.core.ColumnExpr;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

public class ColumnRefRenderer implements Renderer<ColumnExpr> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(ColumnExpr node, RenderContext ctx, SqlWriter w) {
        var quoter = ctx.dialect().quoter();
        var table = node.tableAlias();
        if (table != null && !table.isBlank()) {
            w.append(quoter.quoteIfNeeded(table));
            w.append(".");
        }
        w.append(quoter.quoteIfNeeded(node.name()));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<ColumnExpr> targetType() {
        return ColumnExpr.class;
    }
}
