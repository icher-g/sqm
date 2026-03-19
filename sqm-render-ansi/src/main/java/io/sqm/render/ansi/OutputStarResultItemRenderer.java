package io.sqm.render.ansi;

import io.sqm.core.OutputStarResultItem;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Rejects SQL Server pseudo-row-source stars for dialects that do not support them.
 */
public class OutputStarResultItemRenderer implements Renderer<OutputStarResultItem> {

    /**
     * Creates an output-star renderer.
     */
    public OutputStarResultItemRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(OutputStarResultItem node, RenderContext ctx, SqlWriter w) {
        throw new UnsupportedDialectFeatureException("OUTPUT pseudo-row-source stars", ctx.dialect().name());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<OutputStarResultItem> targetType() {
        return OutputStarResultItem.class;
    }
}
