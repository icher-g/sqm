package io.sqm.render.ansi;

import io.sqm.core.PatternDefinition;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders one row-pattern variable definition.
 */
public class PatternDefinitionRenderer implements Renderer<PatternDefinition> {
    /**
     * Creates a pattern-definition renderer.
     */
    public PatternDefinitionRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(PatternDefinition node, RenderContext ctx, SqlWriter w) {
        PatternRenderSupport.requireSupport(ctx);
        w.append(renderIdentifier(node.variable(), ctx.dialect().quoter()))
            .space().append("AS").space().append(node.condition());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<PatternDefinition> targetType() {
        return PatternDefinition.class;
    }
}
