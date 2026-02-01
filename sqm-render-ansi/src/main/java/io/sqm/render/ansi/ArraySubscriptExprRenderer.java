package io.sqm.render.ansi;

import io.sqm.core.ArraySubscriptExpr;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

public class ArraySubscriptExprRenderer implements Renderer<ArraySubscriptExpr> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(ArraySubscriptExpr node, RenderContext ctx, SqlWriter w) {
        if (!ctx.dialect().capabilities().supports(SqlFeature.ARRAY_SUBSCRIPT)) {
            throw new UnsupportedDialectFeatureException("Array subscript", ctx.dialect().name());
        }
        w.append(node.base()).append("[").append(node.index()).append("]");
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<? extends ArraySubscriptExpr> targetType() {
        return ArraySubscriptExpr.class;
    }
}
