package io.sqm.render.ansi;

import io.sqm.core.CellSelector;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Explicit unsupported boundary for CellSelector.Value.
 */
public class CellSelectorValueRenderer implements Renderer<CellSelector.Value> {
    /**
     * Creates the CellSelector.Value render.
     */
    public CellSelectorValueRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(CellSelector.Value node, RenderContext ctx, SqlWriter w) {
        throw new UnsupportedDialectFeatureException("MODEL CellSelector.Value is not supported by this dialect", ctx.dialect().name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<CellSelector.Value> targetType() {
        return CellSelector.Value.class;
    }
}
