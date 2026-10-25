package io.sqm.render.ansi;

import io.sqm.core.CellAddress;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Explicit unsupported boundary for CellAddress.
 */
public class CellAddressRenderer implements Renderer<CellAddress> {
    /**
     * Creates the CellAddress render.
     */
    public CellAddressRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(CellAddress node, RenderContext ctx, SqlWriter w) {
        throw new UnsupportedDialectFeatureException("MODEL CellAddress is not supported by this dialect", ctx.dialect().name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<CellAddress> targetType() {
        return CellAddress.class;
    }
}
