package io.sqm.render.ansi;

import io.sqm.core.PresenceValueExpr;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Explicit ANSI unsupported boundary for PresenceValueExpr; supporting dialects override it.
 */
public class PresenceValueExprRenderer implements Renderer<PresenceValueExpr> {
    /**
     * Creates the PresenceValueExpr render.
     */
    public PresenceValueExprRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(PresenceValueExpr node, RenderContext ctx, SqlWriter w) {
        throw new UnsupportedDialectFeatureException("MODEL PresenceValueExpr is not supported by this dialect", ctx.dialect().name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<PresenceValueExpr> targetType() {
        return PresenceValueExpr.class;
    }
}
