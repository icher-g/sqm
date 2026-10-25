package io.sqm.render.ansi;

import io.sqm.core.CellRefExpr;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Explicit unsupported boundary for CellRefExpr.
 */
public class CellRefExprRenderer implements Renderer<CellRefExpr> {
    /**
     * Creates the CellRefExpr render.
     */
    public CellRefExprRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(CellRefExpr node, RenderContext ctx, SqlWriter w) {
        throw new UnsupportedDialectFeatureException("MODEL CellRefExpr is not supported by this dialect", ctx.dialect().name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<CellRefExpr> targetType() {
        return CellRefExpr.class;
    }
}
