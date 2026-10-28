package io.sqm.render.ansi;

import io.sqm.core.CellPresentPredicate;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Explicit ANSI unsupported boundary for CellPresentPredicate; supporting dialects override it.
 */
public class CellPresentPredicateRenderer implements Renderer<CellPresentPredicate> {
    /**
     * Creates the CellPresentPredicate render.
     */
    public CellPresentPredicateRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(CellPresentPredicate node, RenderContext ctx, SqlWriter w) {
        throw new UnsupportedDialectFeatureException("MODEL CellPresentPredicate is not supported by this dialect", ctx.dialect().name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<CellPresentPredicate> targetType() {
        return CellPresentPredicate.class;
    }
}
