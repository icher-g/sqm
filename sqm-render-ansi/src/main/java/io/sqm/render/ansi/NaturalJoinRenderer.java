package io.sqm.render.ansi;

import io.sqm.core.NaturalJoin;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

public class NaturalJoinRenderer implements Renderer<NaturalJoin> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(NaturalJoin node, RenderContext ctx, SqlWriter w) {
        w.append("NATURAL JOIN").space();
        w.append(node.right());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<NaturalJoin> targetType() {
        return NaturalJoin.class;
    }
}
