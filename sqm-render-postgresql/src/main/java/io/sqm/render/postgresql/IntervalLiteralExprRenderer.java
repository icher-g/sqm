package io.sqm.render.postgresql;

import io.sqm.core.IntervalLiteralExpr;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders PostgreSQL {@code INTERVAL '...'} literals.
 */
public class IntervalLiteralExprRenderer implements Renderer<IntervalLiteralExpr> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(IntervalLiteralExpr node, RenderContext ctx, SqlWriter w) {
        w.append("INTERVAL ").append(ctx.dialect().formatter().format(node.value()));
        node.qualifier().ifPresent(q -> w.append(" ").append(q));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<IntervalLiteralExpr> targetType() {
        return IntervalLiteralExpr.class;
    }
}
