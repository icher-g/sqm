package io.sqm.render.ansi;

import io.sqm.core.PatternSubset;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders one row-pattern subset declaration.
 */
public class PatternSubsetRenderer implements Renderer<PatternSubset> {
    /**
     * Creates a pattern-subset renderer.
     */
    public PatternSubsetRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(PatternSubset node, RenderContext ctx, SqlWriter w) {
        PatternRenderSupport.requireSupport(ctx);
        var quoter = ctx.dialect().quoter();
        w.append(renderIdentifier(node.name(), quoter)).space().append("=").space().append("(");
        w.comma(node.variables(), quoter).append(")");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<PatternSubset> targetType() {
        return PatternSubset.class;
    }
}
