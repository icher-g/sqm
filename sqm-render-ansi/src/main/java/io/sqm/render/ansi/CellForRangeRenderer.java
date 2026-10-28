package io.sqm.render.ansi;

import io.sqm.core.CellFor;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Explicit ANSI unsupported boundary for CellFor.Range; supporting dialects override it.
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
        throw new UnsupportedDialectFeatureException("MODEL CellFor.Range is not supported by this dialect", ctx.dialect().name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<CellFor.Range> targetType() {
        return CellFor.Range.class;
    }
}
