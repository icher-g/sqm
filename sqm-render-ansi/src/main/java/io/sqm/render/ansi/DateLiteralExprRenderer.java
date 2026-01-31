package io.sqm.render.ansi;

import io.sqm.core.DateLiteralExpr;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders ANSI {@code DATE '...'} literals.
 */
public class DateLiteralExprRenderer implements Renderer<DateLiteralExpr> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(DateLiteralExpr node, RenderContext ctx, SqlWriter w) {
        w.append("DATE ").append(ctx.dialect().formatter().format(node.value()));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<DateLiteralExpr> targetType() {
        return DateLiteralExpr.class;
    }
}
