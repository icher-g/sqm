package io.sqm.render.ansi;

import io.sqm.core.DollarStringLiteralExpr;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders dollar-quoted string literals when supported by the dialect.
 */
public class DollarStringLiteralExprRenderer implements Renderer<DollarStringLiteralExpr> {
    /**
     * Creates a dollar-string literal renderer.
     */
    public DollarStringLiteralExprRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(DollarStringLiteralExpr node, RenderContext ctx, SqlWriter w) {
        if (!ctx.dialect().capabilities().supports(SqlFeature.DOLLAR_STRING_LITERAL)) {
            throw new UnsupportedDialectFeatureException(SqlFeature.DOLLAR_STRING_LITERAL.description(), ctx.dialect().name());
        }
        w.append("$").append(node.tag()).append("$")
            .append(node.value())
            .append("$").append(node.tag()).append("$");
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<DollarStringLiteralExpr> targetType() {
        return DollarStringLiteralExpr.class;
    }
}
