package io.sqm.render.ansi;

import io.sqm.core.CellSelector;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Explicit unsupported boundary for CellSelector.Condition. Oracle support follows in #506.
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
        throw new UnsupportedDialectFeatureException(ctx.dialect().capabilities().supports(SqlFeature.MODEL_CLAUSE) ? "MODEL CellSelector.Condition is not supported yet (#506)" : "MODEL CellSelector.Condition is not supported by this dialect", ctx.dialect().name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<CellSelector.Condition> targetType() {
        return CellSelector.Condition.class;
    }
}
