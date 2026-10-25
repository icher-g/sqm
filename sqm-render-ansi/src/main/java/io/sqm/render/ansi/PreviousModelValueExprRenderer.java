package io.sqm.render.ansi;

import io.sqm.core.PreviousModelValueExpr;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Explicit unsupported boundary for PreviousModelValueExpr. Oracle support follows in #506.
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
        throw new UnsupportedDialectFeatureException(ctx.dialect().capabilities().supports(SqlFeature.MODEL_CLAUSE) ? "MODEL PreviousModelValueExpr is not supported yet (#506)" : "MODEL PreviousModelValueExpr is not supported by this dialect", ctx.dialect().name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<PreviousModelValueExpr> targetType() {
        return PreviousModelValueExpr.class;
    }
}
