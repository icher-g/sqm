package io.sqm.render.ansi;

import io.sqm.core.DollarStringLiteralExpr;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Rejects PostgreSQL dollar-quoted string literals in the ANSI dialect.
 */
public class DollarStringLiteralExprRenderer implements Renderer<DollarStringLiteralExpr> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(DollarStringLiteralExpr node, RenderContext ctx, SqlWriter w) {
        throw new UnsupportedDialectFeatureException("dollar-quoted string", "ANSI");
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
