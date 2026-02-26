package io.sqm.render.ansi;

import io.sqm.core.EscapeStringLiteralExpr;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders escape string literals when supported by the dialect.
 */
public class EscapeStringLiteralExprRenderer implements Renderer<EscapeStringLiteralExpr> {
    /**
     * Creates an escape-string literal renderer.
     */
    public EscapeStringLiteralExprRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(EscapeStringLiteralExpr node, RenderContext ctx, SqlWriter w) {
        if (!ctx.dialect().capabilities().supports(SqlFeature.ESCAPE_STRING_LITERAL)) {
            throw new UnsupportedDialectFeatureException(SqlFeature.ESCAPE_STRING_LITERAL.description(), ctx.dialect().name());
        }
        w.append("E'").append(node.value()).append("'");
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
