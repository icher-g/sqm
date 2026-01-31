package io.sqm.render.postgresql;

import io.sqm.core.DollarStringLiteralExpr;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders PostgreSQL dollar-quoted string literals.
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
