package io.sqm.render.ansi;

import io.sqm.core.ExprSelectItem;
import io.sqm.core.QueryExpr;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

public class ExprSelectItemRenderer implements Renderer<ExprSelectItem> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(ExprSelectItem node, RenderContext ctx, SqlWriter w) {
        w.append(node.expr(), node.expr() instanceof QueryExpr, true);

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
    public Class<ExprSelectItem> targetType() {
        return ExprSelectItem.class;
    }
}
