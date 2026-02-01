package io.sqm.render.ansi;

import io.sqm.core.BinaryOperatorExpr;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

public class BinaryOperatorExprRenderer implements Renderer<BinaryOperatorExpr> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(BinaryOperatorExpr node, RenderContext ctx, SqlWriter w) {
        if (!ctx.dialect().capabilities().supports(SqlFeature.CUSTOM_OPERATOR)) {
            throw new UnsupportedDialectFeatureException("Binary operator " + node.operator(), ctx.dialect().name());
        }
        w.append(node.left()).space().append(node.operator()).space().append(node.right());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<? extends BinaryOperatorExpr> targetType() {
        return BinaryOperatorExpr.class;
    }
}
