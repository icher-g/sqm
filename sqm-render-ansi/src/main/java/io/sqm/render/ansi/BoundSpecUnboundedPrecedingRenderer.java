package io.sqm.render.ansi;

import io.sqm.core.BoundSpec;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

public class BoundSpecUnboundedPrecedingRenderer implements Renderer<BoundSpec.UnboundedPreceding> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(BoundSpec.UnboundedPreceding node, RenderContext ctx, SqlWriter w) {
        w.append("UNBOUNDED PRECEDING");
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<BoundSpec.UnboundedPreceding> targetType() {
        return BoundSpec.UnboundedPreceding.class;
    }
}
