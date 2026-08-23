package io.sqm.render.ansi;

import io.sqm.core.MatchPattern;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders a row-pattern partition anchor.
 */
public class PatternAnchorRenderer implements Renderer<MatchPattern.Anchor> {
    /**
     * Creates a pattern-anchor renderer.
     */
    public PatternAnchorRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(MatchPattern.Anchor node, RenderContext ctx, SqlWriter w) {
        PatternRenderSupport.requireSupport(ctx);
        w.append(node.kind() == MatchPattern.Anchor.Kind.START ? "^" : "$");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<MatchPattern.Anchor> targetType() {
        return MatchPattern.Anchor.class;
    }
}
