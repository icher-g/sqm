package io.sqm.render.ansi;

import io.sqm.core.MatchPattern;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders a {@code PERMUTE(...)} pattern.
 */
public class PatternPermutationRenderer implements Renderer<MatchPattern.Permutation> {
    /**
     * Creates a pattern-permutation renderer.
     */
    public PatternPermutationRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(MatchPattern.Permutation node, RenderContext ctx, SqlWriter w) {
        PatternRenderSupport.requireSupport(ctx);
        w.append("PERMUTE(");
        for (int i = 0; i < node.elements().size(); i++) {
            if (i > 0) w.append(",").space();
            PatternRenderSupport.renderChild(node.elements().get(i), 0, ctx, w);
        }
        w.append(")");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<MatchPattern.Permutation> targetType() {
        return MatchPattern.Permutation.class;
    }
}
