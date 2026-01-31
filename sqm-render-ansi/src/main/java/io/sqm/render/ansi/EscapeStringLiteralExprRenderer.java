package io.sqm.render.ansi;

import io.sqm.core.EscapeStringLiteralExpr;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Rejects PostgreSQL escape string literals in the ANSI dialect.
 */
public class EscapeStringLiteralExprRenderer implements Renderer<EscapeStringLiteralExpr> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(EscapeStringLiteralExpr node, RenderContext ctx, SqlWriter w) {
        throw new UnsupportedDialectFeatureException("E'...'", "ANSI");
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<EscapeStringLiteralExpr> targetType() {
        return EscapeStringLiteralExpr.class;
    }
}
