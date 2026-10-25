package io.sqm.render.ansi;

import io.sqm.core.CellTarget;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Explicit unsupported boundary for CellTarget.
 */
public class CellTargetRenderer implements Renderer<CellTarget> {
    /**
     * Creates the CellTarget render.
     */
    public CellTargetRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(CellTarget node, RenderContext ctx, SqlWriter w) {
        throw new UnsupportedDialectFeatureException("MODEL CellTarget is not supported by this dialect", ctx.dialect().name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<CellTarget> targetType() {
        return CellTarget.class;
    }
}
