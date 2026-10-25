package io.sqm.render.ansi;

import io.sqm.core.MainModel;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Explicit unsupported boundary for MainModel.
 */
public class MainModelRenderer implements Renderer<MainModel> {
    /**
     * Creates the MainModel render.
     */
    public MainModelRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(MainModel node, RenderContext ctx, SqlWriter w) {
        throw new UnsupportedDialectFeatureException("MODEL MainModel is not supported by this dialect", ctx.dialect().name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<MainModel> targetType() {
        return MainModel.class;
    }
}
