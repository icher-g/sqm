package io.sqm.render.ansi;

import io.sqm.core.AtTimeZoneExpr;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * ANSI renderer for PostgreSQL {@code AT TIME ZONE} expression.
 * <p>
 * Renders expressions of the form: {@code <timestamp_expr> AT TIME ZONE <timezone_expr>}
 */
public class AtTimeZoneExprRenderer implements Renderer<AtTimeZoneExpr> {

    /**
     * Creates an AT TIME ZONE renderer.
     */
    public AtTimeZoneExprRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}}.
     *
     * @param node a node to render
     * @param ctx  a render context
     * @param w    a writer
     */
    @Override
    public void render(AtTimeZoneExpr node, RenderContext ctx, SqlWriter w) {
        if (!ctx.dialect().capabilities().supports(SqlFeature.AT_TIME_ZONE)) {
            throw new UnsupportedOperationException(
                "AT TIME ZONE is not supported by dialect: " + ctx.dialect().name()
            );
        }

        w.append(node.timestamp())
            .space()
            .append("AT TIME ZONE")
            .space()
            .append(node.timezone());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return the entity type to be handled by the handler
     */
    @Override
    public Class<? extends AtTimeZoneExpr> targetType() {
        return AtTimeZoneExpr.class;
    }
}
