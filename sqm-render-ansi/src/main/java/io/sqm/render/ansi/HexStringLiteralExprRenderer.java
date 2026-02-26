package io.sqm.render.ansi;

import io.sqm.core.HexStringLiteralExpr;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders ANSI hex string literals (e.g., {@code X'FF'}).
 */
public class HexStringLiteralExprRenderer implements Renderer<HexStringLiteralExpr> {
    /**
     * Creates a hex-string literal renderer.
     */
    public HexStringLiteralExprRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(HexStringLiteralExpr node, RenderContext ctx, SqlWriter w) {
        w.append("X'").append(escapeQuotes(node.value())).append("'");
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<HexStringLiteralExpr> targetType() {
        return HexStringLiteralExpr.class;
    }

    private String escapeQuotes(String value) {
        return value.replace("'", "''");
    }
}
