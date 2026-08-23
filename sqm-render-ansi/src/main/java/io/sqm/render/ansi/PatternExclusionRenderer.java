package io.sqm.render.ansi;

import io.sqm.core.MatchPattern;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders a row-pattern exclusion.
 */
public class PatternExclusionRenderer implements Renderer<MatchPattern.Exclusion> {
    /**
     * Creates a pattern-exclusion renderer.
     */
    public PatternExclusionRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(MatchPattern.Exclusion node, RenderContext ctx, SqlWriter w) {
        PatternRenderSupport.requireSupport(ctx);
        w.append("{-").space();
        PatternRenderSupport.renderChild(node.pattern(), 0, ctx, w);
        w.space().append("-}");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<MatchPattern.Exclusion> targetType() {
        return MatchPattern.Exclusion.class;
    }
}
