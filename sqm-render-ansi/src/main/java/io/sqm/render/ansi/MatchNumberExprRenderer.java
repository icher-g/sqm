package io.sqm.render.ansi;

import io.sqm.core.MatchNumberExpr;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders the row-pattern match ordinal expression.
 */
public class MatchNumberExprRenderer implements Renderer<MatchNumberExpr> {
    /**
     * Creates a match-number renderer.
     */
    public MatchNumberExprRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(MatchNumberExpr node, RenderContext ctx, SqlWriter w) {
        PatternRenderSupport.requireSupport(ctx);
        w.append("MATCH_NUMBER()");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<MatchNumberExpr> targetType() {
        return MatchNumberExpr.class;
    }
}
