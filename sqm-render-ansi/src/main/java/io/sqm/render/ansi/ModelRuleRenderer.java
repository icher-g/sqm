package io.sqm.render.ansi;

import io.sqm.core.ModelRule;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Explicit unsupported boundary for ModelRule.
 */
public class ModelRuleRenderer implements Renderer<ModelRule> {
    /**
     * Creates the ModelRule render.
     */
    public ModelRuleRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(ModelRule node, RenderContext ctx, SqlWriter w) {
        throw new UnsupportedDialectFeatureException("MODEL ModelRule is not supported by this dialect", ctx.dialect().name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<ModelRule> targetType() {
        return ModelRule.class;
    }
}
