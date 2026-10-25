package io.sqm.render.ansi;

import io.sqm.core.ModelAggregateExpr;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Explicit unsupported boundary for ModelAggregateExpr. Oracle support follows in #506.
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
        throw new UnsupportedDialectFeatureException(ctx.dialect().capabilities().supports(SqlFeature.MODEL_CLAUSE) ? "MODEL ModelAggregateExpr is not supported yet (#506)" : "MODEL ModelAggregateExpr is not supported by this dialect", ctx.dialect().name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<ModelAggregateExpr> targetType() {
        return ModelAggregateExpr.class;
    }
}
