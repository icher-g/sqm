package io.sqm.render.ansi;

import io.sqm.core.FrameSpec;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders frame specifications with a single bound.
 */
public class FrameSpecSingleRenderer implements Renderer<FrameSpec.Single> {
    /**
     * Creates a single-bound frame renderer.
     */
    public FrameSpecSingleRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(FrameSpec.Single node, RenderContext ctx, SqlWriter w) {
        w.append(node.unit().name()).space().append(node.bound());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<FrameSpec.Single> targetType() {
        return FrameSpec.Single.class;
    }
}
