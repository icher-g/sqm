package io.sqm.render.ansi;

import io.sqm.core.PatternColumnExpr;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders a pattern-variable-qualified column.
 */
public class PatternColumnExprRenderer implements Renderer<PatternColumnExpr> {
    /**
     * Creates a pattern-column renderer.
     */
    public PatternColumnExprRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(PatternColumnExpr node, RenderContext ctx, SqlWriter w) {
        PatternRenderSupport.requireSupport(ctx);
        var quoter = ctx.dialect().quoter();
        w.append(renderIdentifier(node.variable(), quoter)).append(".")
            .append(renderIdentifier(node.column(), quoter));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<PatternColumnExpr> targetType() {
        return PatternColumnExpr.class;
    }
}
