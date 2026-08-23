package io.sqm.render.ansi;

import io.sqm.core.PatternMeasure;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders one named row-pattern measure.
 */
public class PatternMeasureRenderer implements Renderer<PatternMeasure> {
    /**
     * Creates a pattern-measure renderer.
     */
    public PatternMeasureRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(PatternMeasure node, RenderContext ctx, SqlWriter w) {
        PatternRenderSupport.requireSupport(ctx);
        w.append(node.expression()).space().append("AS").space()
            .append(renderIdentifier(node.alias(), ctx.dialect().quoter()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<PatternMeasure> targetType() {
        return PatternMeasure.class;
    }
}
