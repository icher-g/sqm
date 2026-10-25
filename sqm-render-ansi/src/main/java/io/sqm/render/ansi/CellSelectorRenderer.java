package io.sqm.render.ansi;

import io.sqm.core.CellSelector;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Explicit unsupported boundary for CellSelector.
 */
public class CellSelectorRenderer implements Renderer<CellSelector> {
    /**
     * Creates the CellSelector render.
     */
    public CellSelectorRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(CellSelector node, RenderContext ctx, SqlWriter w) {
        throw new UnsupportedDialectFeatureException("MODEL CellSelector is not supported by this dialect", ctx.dialect().name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<CellSelector> targetType() {
        return CellSelector.class;
    }
}
