package io.sqm.render.ansi;

import io.sqm.core.UsingJoin;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

public class UsingJoinRenderer implements Renderer<UsingJoin> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(UsingJoin node, RenderContext ctx, SqlWriter w) {
        w.append("USING").space()
            .append("(")
            .append(ctx.dialect().formatter().format(node.usingColumns()))
            .append(")");
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<UsingJoin> targetType() {
        return UsingJoin.class;
    }
}
