package io.sqm.render.ansi;

import io.sqm.core.PresenceValueExpr;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Explicit unsupported boundary for PresenceValueExpr. Oracle support follows in #506.
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
        throw new UnsupportedDialectFeatureException(ctx.dialect().capabilities().supports(SqlFeature.MODEL_CLAUSE) ? "MODEL PresenceValueExpr is not supported yet (#506)" : "MODEL PresenceValueExpr is not supported by this dialect", ctx.dialect().name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<PresenceValueExpr> targetType() {
        return PresenceValueExpr.class;
    }
}
