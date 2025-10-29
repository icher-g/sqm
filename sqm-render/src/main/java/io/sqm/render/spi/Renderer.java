package io.sqm.render.spi;

import io.sqm.core.Node;
import io.sqm.core.repos.Handler;
import io.sqm.render.SqlWriter;

/**
 * A base interface for all renderers.
 *
 * @param <T>
 */
public interface Renderer<T extends Node> extends Handler<T> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    void render(T node, RenderContext ctx, SqlWriter w);
}
