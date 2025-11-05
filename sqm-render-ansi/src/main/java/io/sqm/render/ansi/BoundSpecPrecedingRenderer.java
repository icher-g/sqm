package io.sqm.render.ansi;

import io.sqm.core.BoundSpec;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

public class BoundSpecPrecedingRenderer implements Renderer<BoundSpec.Preceding> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(BoundSpec.Preceding node, RenderContext ctx, SqlWriter w) {
        w.append(node.expr()).space().append("PRECEDING");
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<BoundSpec.Preceding> targetType() {
        return BoundSpec.Preceding.class;
    }
}
