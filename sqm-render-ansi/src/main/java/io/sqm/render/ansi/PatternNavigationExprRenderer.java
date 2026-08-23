package io.sqm.render.ansi;

import io.sqm.core.PatternNavigationExpr;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders a row-pattern navigation expression.
 */
public class PatternNavigationExprRenderer implements Renderer<PatternNavigationExpr> {
    /**
     * Creates a pattern-navigation renderer.
     */
    public PatternNavigationExprRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(PatternNavigationExpr node, RenderContext ctx, SqlWriter w) {
        PatternRenderSupport.requireSupport(ctx);
        w.append(node.kind().name()).append("(").append(node.expression());
        if (node.offset() != null) w.append(",").space().append(node.offset());
        w.append(")");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<PatternNavigationExpr> targetType() {
        return PatternNavigationExpr.class;
    }
}
