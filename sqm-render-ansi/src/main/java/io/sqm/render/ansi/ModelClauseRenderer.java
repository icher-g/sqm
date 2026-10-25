package io.sqm.render.ansi;

import io.sqm.core.ModelClause;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Explicit unsupported boundary for ModelClause.
 */
public class ModelClauseRenderer implements Renderer<ModelClause> {
    /**
     * Creates the ModelClause render.
     */
    public ModelClauseRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(ModelClause node, RenderContext ctx, SqlWriter w) {
        throw new UnsupportedDialectFeatureException("MODEL ModelClause is not supported by this dialect", ctx.dialect().name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<ModelClause> targetType() {
        return ModelClause.class;
    }
}
