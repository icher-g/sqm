package io.sqm.render.ansi;

import io.sqm.core.CurrentDimensionExpr;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Explicit ANSI unsupported boundary for CurrentDimensionExpr; supporting dialects override it.
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
        throw new UnsupportedDialectFeatureException("MODEL CurrentDimensionExpr is not supported by this dialect", ctx.dialect().name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<CurrentDimensionExpr> targetType() {
        return CurrentDimensionExpr.class;
    }
}
