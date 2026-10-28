package io.sqm.render.ansi;

import io.sqm.core.PreviousModelValueExpr;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Explicit ANSI unsupported boundary for PreviousModelValueExpr; supporting dialects override it.
 */
public class PreviousModelValueExprRenderer implements Renderer<PreviousModelValueExpr> {
    /**
     * Creates the PreviousModelValueExpr render.
     */
    public PreviousModelValueExprRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(PreviousModelValueExpr node, RenderContext ctx, SqlWriter w) {
        throw new UnsupportedDialectFeatureException("MODEL PreviousModelValueExpr is not supported by this dialect", ctx.dialect().name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<PreviousModelValueExpr> targetType() {
        return PreviousModelValueExpr.class;
    }
}
