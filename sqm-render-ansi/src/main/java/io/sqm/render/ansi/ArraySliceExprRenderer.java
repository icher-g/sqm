package io.sqm.render.ansi;

import io.sqm.core.ArraySliceExpr;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

public class ArraySliceExprRenderer implements Renderer<ArraySliceExpr> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(ArraySliceExpr node, RenderContext ctx, SqlWriter w) {
        if (!ctx.dialect().capabilities().supports(SqlFeature.ARRAY_SLICE)) {
            throw new UnsupportedDialectFeatureException("Array slice", ctx.dialect().name());
        }
        w.append(node.base()).append("[");
        if (node.from().isPresent()) {
            w.append(node.from().get());
        }
        w.append(":");
        if (node.to().isPresent()) {
            w.append(node.to().get());
        }
        w.append("]");
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<? extends ArraySliceExpr> targetType() {
        return ArraySliceExpr.class;
    }
}
