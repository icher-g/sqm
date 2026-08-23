package io.sqm.render.ansi;

import io.sqm.core.MatchPattern;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders the semantic empty row pattern.
 */
public class EmptyPatternRenderer implements Renderer<MatchPattern.Empty> {
    /**
     * Creates an empty-pattern renderer.
     */
    public EmptyPatternRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(MatchPattern.Empty node, RenderContext ctx, SqlWriter w) {
        PatternRenderSupport.requireSupport(ctx);
        w.append("()");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<MatchPattern.Empty> targetType() {
        return MatchPattern.Empty.class;
    }
}
