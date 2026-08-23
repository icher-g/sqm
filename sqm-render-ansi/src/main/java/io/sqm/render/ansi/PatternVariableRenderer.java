package io.sqm.render.ansi;

import io.sqm.core.MatchPattern;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders a primary row-pattern variable.
 */
public class PatternVariableRenderer implements Renderer<MatchPattern.Variable> {
    /**
     * Creates a pattern-variable renderer.
     */
    public PatternVariableRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(MatchPattern.Variable node, RenderContext ctx, SqlWriter w) {
        PatternRenderSupport.requireSupport(ctx);
        w.append(renderIdentifier(node.name(), ctx.dialect().quoter()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<MatchPattern.Variable> targetType() {
        return MatchPattern.Variable.class;
    }
}
