package io.sqm.render.ansi;

import io.sqm.core.ModelAggregateExpr;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Explicit ANSI unsupported boundary for ModelAggregateExpr; supporting dialects override it.
 */
public class ModelAggregateExprRenderer implements Renderer<ModelAggregateExpr> {
    /**
     * Creates the ModelAggregateExpr render.
     */
    public ModelAggregateExprRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(ModelAggregateExpr node, RenderContext ctx, SqlWriter w) {
        throw new UnsupportedDialectFeatureException("MODEL ModelAggregateExpr is not supported by this dialect", ctx.dialect().name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<ModelAggregateExpr> targetType() {
        return ModelAggregateExpr.class;
    }
}
