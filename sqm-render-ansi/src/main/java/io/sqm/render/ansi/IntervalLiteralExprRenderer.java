package io.sqm.render.ansi;

import io.sqm.core.IntervalLiteralExpr;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders {@code INTERVAL '...'} literals when supported by the dialect.
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
        if (!ctx.dialect().capabilities().supports(SqlFeature.INTERVAL_LITERAL)) {
            throw new UnsupportedDialectFeatureException(SqlFeature.INTERVAL_LITERAL.description(), ctx.dialect().name());
        }
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
