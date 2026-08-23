package io.sqm.render.ansi;

import io.sqm.core.PatternEvaluationExpr;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders a {@code RUNNING} or {@code FINAL} pattern expression.
 */
public class PatternEvaluationExprRenderer implements Renderer<PatternEvaluationExpr> {
    /**
     * Creates a pattern-evaluation renderer.
     */
    public PatternEvaluationExprRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(PatternEvaluationExpr node, RenderContext ctx, SqlWriter w) {
        PatternRenderSupport.requireSupport(ctx);
        w.append(node.mode().name()).space().append(node.expression());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<PatternEvaluationExpr> targetType() {
        return PatternEvaluationExpr.class;
    }
}
