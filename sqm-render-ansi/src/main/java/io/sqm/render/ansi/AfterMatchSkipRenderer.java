package io.sqm.render.ansi;

import io.sqm.core.AfterMatchSkip;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders an {@code AFTER MATCH SKIP} specification.
 */
public class AfterMatchSkipRenderer implements Renderer<AfterMatchSkip> {
    /**
     * Creates an after-match-skip renderer.
     */
    public AfterMatchSkipRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(AfterMatchSkip node, RenderContext ctx, SqlWriter w) {
        PatternRenderSupport.requireSupport(ctx);
        w.append("AFTER MATCH SKIP ");
        switch (node.kind()) {
            case PAST_LAST_ROW -> w.append("PAST LAST ROW");
            case TO_NEXT_ROW -> w.append("TO NEXT ROW");
            case TO_VARIABLE -> {
                w.append("TO").space();
                if (node.position() != AfterMatchSkip.Position.DEFAULT) {
                    w.append(node.position().name()).space();
                }
                w.append(renderIdentifier(node.variable(), ctx.dialect().quoter()));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<AfterMatchSkip> targetType() {
        return AfterMatchSkip.class;
    }
}
