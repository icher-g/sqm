package io.sqm.render.ansi;

import io.sqm.core.IterationSpec;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Explicit ANSI unsupported boundary for IterationSpec; supporting dialects override it.
 */
public class IterationSpecRenderer implements Renderer<IterationSpec> {
    /**
     * Creates the IterationSpec render.
     */
    public IterationSpecRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(IterationSpec node, RenderContext ctx, SqlWriter w) {
        throw new UnsupportedDialectFeatureException("MODEL IterationSpec is not supported by this dialect", ctx.dialect().name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<IterationSpec> targetType() {
        return IterationSpec.class;
    }
}
