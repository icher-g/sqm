package io.sqm.render.ansi;

import io.sqm.core.OutputColumnExpr;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Rejects SQL Server pseudo-columns for dialects that do not support them.
 */
public class OutputColumnExprRenderer implements Renderer<OutputColumnExpr> {

    /**
     * Creates an result-column renderer.
     */
    public OutputColumnExprRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(OutputColumnExpr node, RenderContext ctx, SqlWriter w) {
        throw new UnsupportedDialectFeatureException("OUTPUT pseudo-columns", ctx.dialect().name());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<OutputColumnExpr> targetType() {
        return OutputColumnExpr.class;
    }
}