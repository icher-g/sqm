package io.sqm.render.ansi;

import io.sqm.core.OnJoin;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

public class OnJoinRenderer implements Renderer<OnJoin> {

    private final JoinKindRenderer kindRenderer = new JoinKindRenderer();

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(OnJoin node, RenderContext ctx, SqlWriter w) {
        kindRenderer.render(node.kind(), ctx, w);
        w.space().append(node.right()).space().append("ON").space().append(node.on());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<OnJoin> targetType() {
        return OnJoin.class;
    }
}
