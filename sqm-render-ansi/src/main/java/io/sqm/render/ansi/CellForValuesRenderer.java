package io.sqm.render.ansi;

import io.sqm.core.CellFor;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Explicit ANSI unsupported boundary for CellFor.Values; supporting dialects override it.
 */
public class CellForValuesRenderer implements Renderer<CellFor.Values> {
    /**
     * Creates the CellFor.Values render.
     */
    public CellForValuesRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(CellFor.Values node, RenderContext ctx, SqlWriter w) {
        throw new UnsupportedDialectFeatureException("MODEL CellFor.Values is not supported by this dialect", ctx.dialect().name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<CellFor.Values> targetType() {
        return CellFor.Values.class;
    }
}
