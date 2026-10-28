package io.sqm.render.ansi;

import io.sqm.core.CellFor;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Explicit ANSI unsupported boundary for CellFor; supporting dialects override it.
 */
public class CellForRenderer implements Renderer<CellFor> {
    /**
     * Creates the CellFor render.
     */
    public CellForRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(CellFor node, RenderContext ctx, SqlWriter w) {
        throw new UnsupportedDialectFeatureException("MODEL CellFor is not supported by this dialect", ctx.dialect().name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<CellFor> targetType() {
        return CellFor.class;
    }
}
