package io.sqm.render.postgresql;

import io.sqm.core.EscapeStringLiteralExpr;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders PostgreSQL escape string literals.
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
