package io.sqm.render.ansi;

import io.sqm.core.ModelColumn;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Explicit unsupported boundary for ModelColumn.
 */
public class ModelColumnRenderer implements Renderer<ModelColumn> {
    /**
     * Creates the ModelColumn render.
     */
    public ModelColumnRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(ModelColumn node, RenderContext ctx, SqlWriter w) {
        throw new UnsupportedDialectFeatureException("MODEL ModelColumn is not supported by this dialect", ctx.dialect().name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<ModelColumn> targetType() {
        return ModelColumn.class;
    }
}
