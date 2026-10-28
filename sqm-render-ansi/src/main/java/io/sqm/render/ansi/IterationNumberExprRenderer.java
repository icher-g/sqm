package io.sqm.render.ansi;

import io.sqm.core.IterationNumberExpr;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Explicit ANSI unsupported boundary for IterationNumberExpr; supporting dialects override it.
 */
public class IterationNumberExprRenderer implements Renderer<IterationNumberExpr> {
    /**
     * Creates the IterationNumberExpr render.
     */
    public IterationNumberExprRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(IterationNumberExpr node, RenderContext ctx, SqlWriter w) {
        throw new UnsupportedDialectFeatureException("MODEL IterationNumberExpr is not supported by this dialect", ctx.dialect().name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<IterationNumberExpr> targetType() {
        return IterationNumberExpr.class;
    }
}
