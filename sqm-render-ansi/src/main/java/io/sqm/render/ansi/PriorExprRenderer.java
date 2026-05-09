package io.sqm.render.ansi;

import io.sqm.core.PriorExpr;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders {@code PRIOR} expressions, or rejects them when unsupported.
 */
public class PriorExprRenderer implements Renderer<PriorExpr> {
    /**
     * Creates a prior-expression renderer.
     */
    public PriorExprRenderer() {
    }

    /**
     * Rejects {@code PRIOR} rendering for unsupported dialects.
     *
     * @param node prior expression
     * @param ctx render context
     * @param w SQL writer
     */
    @Override
    public void render(PriorExpr node, RenderContext ctx, SqlWriter w) {
        throw new UnsupportedDialectFeatureException(SqlFeature.HIERARCHICAL_QUERY.description(), ctx.dialect().name());
    }

    /**
     * Returns this renderer target type.
     *
     * @return {@link PriorExpr} class
     */
    @Override
    public Class<PriorExpr> targetType() {
        return PriorExpr.class;
    }
}
