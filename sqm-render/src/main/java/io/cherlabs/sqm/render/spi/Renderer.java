package io.cherlabs.sqm.render.spi;

import io.cherlabs.sqm.core.Entity;
import io.cherlabs.sqm.core.repos.Handler;
import io.cherlabs.sqm.render.SqlWriter;

/**
 * A base interface for all renderers.
 *
 * @param <T>
 */
public interface Renderer<T extends Entity> extends Handler<T> {
    /**
     * Renders the entity into an {@link SqlWriter}.
     *
     * @param entity an entity to render.
     * @param ctx    a render context.
     * @param w      a writer.
     */
    void render(T entity, RenderContext ctx, SqlWriter w);
}
