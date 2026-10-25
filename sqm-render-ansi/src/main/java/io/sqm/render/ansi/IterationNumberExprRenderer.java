package io.sqm.render.ansi;

import io.sqm.core.IterationNumberExpr;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Explicit unsupported boundary for IterationNumberExpr. Oracle support follows in #506.
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
        throw new UnsupportedDialectFeatureException(ctx.dialect().capabilities().supports(SqlFeature.MODEL_CLAUSE) ? "MODEL IterationNumberExpr is not supported yet (#506)" : "MODEL IterationNumberExpr is not supported by this dialect", ctx.dialect().name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<IterationNumberExpr> targetType() {
        return IterationNumberExpr.class;
    }
}
