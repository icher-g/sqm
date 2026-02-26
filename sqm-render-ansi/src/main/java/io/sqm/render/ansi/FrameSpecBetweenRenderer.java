package io.sqm.render.ansi;

import io.sqm.core.FrameSpec;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders frame specifications that use BETWEEN bounds.
 */
public class FrameSpecBetweenRenderer implements Renderer<FrameSpec.Between> {
    /**
     * Creates a BETWEEN-frame renderer.
     */
    public FrameSpecBetweenRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(FrameSpec.Between node, RenderContext ctx, SqlWriter w) {
        w.append(node.unit().name()).space();
        w.append("BETWEEN").space();
        w.append(node.start()).space();
        w.append("AND").space();
        w.append(node.end());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<FrameSpec.Between> targetType() {
        return FrameSpec.Between.class;
    }
}
