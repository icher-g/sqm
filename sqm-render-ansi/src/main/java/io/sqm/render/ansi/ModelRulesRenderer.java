package io.sqm.render.ansi;

import io.sqm.core.ModelRules;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Explicit unsupported boundary for ModelRules.
 */
public class ModelRulesRenderer implements Renderer<ModelRules> {
    /**
     * Creates the ModelRules render.
     */
    public ModelRulesRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(ModelRules node, RenderContext ctx, SqlWriter w) {
        throw new UnsupportedDialectFeatureException("MODEL ModelRules is not supported by this dialect", ctx.dialect().name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<ModelRules> targetType() {
        return ModelRules.class;
    }
}
