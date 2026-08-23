package io.sqm.render.ansi;

import io.sqm.core.MatchPattern;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders row-pattern alternation.
 */
public class PatternAlternationRenderer implements Renderer<MatchPattern.Alternation> {
    /**
     * Creates a pattern-alternation renderer.
     */
    public PatternAlternationRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(MatchPattern.Alternation node, RenderContext ctx, SqlWriter w) {
        PatternRenderSupport.requireSupport(ctx);
        for (int i = 0; i < node.alternatives().size(); i++) {
            if (i > 0) w.space().append("|").space();
            PatternRenderSupport.renderChild(node.alternatives().get(i), 1, ctx, w);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<MatchPattern.Alternation> targetType() {
        return MatchPattern.Alternation.class;
    }
}
