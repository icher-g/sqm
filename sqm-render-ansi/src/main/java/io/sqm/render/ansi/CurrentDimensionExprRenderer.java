package io.sqm.render.ansi;

import io.sqm.core.CurrentDimensionExpr;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Explicit unsupported boundary for CurrentDimensionExpr. Oracle support follows in #506.
 */
public class CurrentDimensionExprRenderer implements Renderer<CurrentDimensionExpr> {
    /**
     * Creates the CurrentDimensionExpr render.
     */
    public CurrentDimensionExprRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(CurrentDimensionExpr node, RenderContext ctx, SqlWriter w) {
        throw new UnsupportedDialectFeatureException(ctx.dialect().capabilities().supports(SqlFeature.MODEL_CLAUSE) ? "MODEL CurrentDimensionExpr is not supported yet (#506)" : "MODEL CurrentDimensionExpr is not supported by this dialect", ctx.dialect().name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<CurrentDimensionExpr> targetType() {
        return CurrentDimensionExpr.class;
    }
}
