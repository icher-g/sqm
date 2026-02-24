package io.sqm.render.ansi;

import io.sqm.core.OverSpec;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

public class OverSpecRefRenderer implements Renderer<OverSpec.Ref> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(OverSpec.Ref node, RenderContext ctx, SqlWriter w) {
        w.append(renderIdentifier(node.windowName(), ctx.dialect().quoter()));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<OverSpec.Ref> targetType() {
        return OverSpec.Ref.class;
    }
}
