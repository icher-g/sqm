package io.sqm.render.ansi;

import io.sqm.core.SampledTable;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Rejects sampled tables for dialects without table sampling support.
 */
public class SampledTableRenderer implements Renderer<SampledTable> {
    /**
     * Creates a sampled table renderer.
     */
    public SampledTableRenderer() {
    }

    /**
     * Renders a sampled table.
     *
     * @param node sampled table
     * @param ctx render context
     * @param w SQL writer
     */
    @Override
    public void render(SampledTable node, RenderContext ctx, SqlWriter w) {
        throw new UnsupportedDialectFeatureException("table sampling", ctx.dialect().name());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return sampled table type
     */
    @Override
    public Class<SampledTable> targetType() {
        return SampledTable.class;
    }
}
