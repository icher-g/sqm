package io.sqm.render.ansi;

import io.sqm.core.CellSelector;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Explicit ANSI unsupported boundary for CellSelector.Condition; supporting dialects override it.
 */
public class CellSelectorConditionRenderer implements Renderer<CellSelector.Condition> {
    /**
     * Creates the CellSelector.Condition render.
     */
    public CellSelectorConditionRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(CellSelector.Condition node, RenderContext ctx, SqlWriter w) {
        throw new UnsupportedDialectFeatureException("MODEL CellSelector.Condition is not supported by this dialect", ctx.dialect().name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<CellSelector.Condition> targetType() {
        return CellSelector.Condition.class;
    }
}
