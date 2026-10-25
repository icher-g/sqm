package io.sqm.render.ansi;

import io.sqm.core.CellFor;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Explicit unsupported boundary for CellFor.Range. Oracle support follows in #506.
 */
public class CellForRangeRenderer implements Renderer<CellFor.Range> {
    /**
     * Creates the CellFor.Range render.
     */
    public CellForRangeRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(CellFor.Range node, RenderContext ctx, SqlWriter w) {
        throw new UnsupportedDialectFeatureException(ctx.dialect().capabilities().supports(SqlFeature.MODEL_CLAUSE) ? "MODEL CellFor.Range is not supported yet (#506)" : "MODEL CellFor.Range is not supported by this dialect", ctx.dialect().name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<CellFor.Range> targetType() {
        return CellFor.Range.class;
    }
}
