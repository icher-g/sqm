package io.sqm.render.ansi;

import io.sqm.core.CrossJoin;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders CROSS JOIN clauses.
 */
public class CrossJoinRenderer implements Renderer<CrossJoin> {
    /**
     * Creates a cross-join renderer.
     */
    public CrossJoinRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(CrossJoin node, RenderContext ctx, SqlWriter w) {
        w.append("CROSS JOIN").space();
        w.append(node.right());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<CrossJoin> targetType() {
        return CrossJoin.class;
    }
}
