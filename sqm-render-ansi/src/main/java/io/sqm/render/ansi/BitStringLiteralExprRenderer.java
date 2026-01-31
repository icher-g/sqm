package io.sqm.render.ansi;

import io.sqm.core.BitStringLiteralExpr;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders ANSI bit string literals (e.g., {@code B'1010'}).
 */
public class BitStringLiteralExprRenderer implements Renderer<BitStringLiteralExpr> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(BitStringLiteralExpr node, RenderContext ctx, SqlWriter w) {
        w.append("B'").append(escapeQuotes(node.value())).append("'");
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<BitStringLiteralExpr> targetType() {
        return BitStringLiteralExpr.class;
    }

    private String escapeQuotes(String value) {
        return value.replace("'", "''");
    }
}
