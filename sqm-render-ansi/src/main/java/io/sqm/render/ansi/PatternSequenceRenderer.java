package io.sqm.render.ansi;

import io.sqm.core.MatchPattern;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders row-pattern concatenation.
 */
public class PatternSequenceRenderer implements Renderer<MatchPattern.Sequence> {
    /**
     * Creates a pattern-sequence renderer.
     */
    public PatternSequenceRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(MatchPattern.Sequence node, RenderContext ctx, SqlWriter w) {
        PatternRenderSupport.requireSupport(ctx);
        for (int i = 0; i < node.elements().size(); i++) {
            if (i > 0) w.space();
            PatternRenderSupport.renderChild(node.elements().get(i), 2, ctx, w);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<MatchPattern.Sequence> targetType() {
        return MatchPattern.Sequence.class;
    }
}
