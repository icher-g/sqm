package io.cherlabs.sqlmodel.render;

import io.cherlabs.sqlmodel.core.Entity;
import io.cherlabs.sqlmodel.core.repos.Handler;
import io.cherlabs.sqlmodel.render.spi.RenderContext;

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
