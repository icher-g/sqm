package io.cherlabs.sqlmodel.render.repos;

import io.cherlabs.sqlmodel.core.Entity;
import io.cherlabs.sqlmodel.render.Renderer;
import io.cherlabs.sqlmodel.render.spi.RenderersRepository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A default implementation of {@link RenderersRepository}.
 */
public final class DefaultRenderersRepository implements RenderersRepository {

    private final Map<Class<?>, Renderer<?>> renderers = new ConcurrentHashMap<>();

    /**
     * Gets a {@link Renderer}.
     *
     * @param type the type of the class which renderer is needed.
     * @param <T>  the type of the entity.
     * @return a renderer if found or NULL otherwise.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends Entity> Renderer<T> get(Class<T> type) {
        var p = renderers.get(type);
        if (p != null) {
            return (Renderer<T>) p;
        }
        return null;
    }

    /**
     * Registers a {@link Renderer} for the provided entity type.
     *
     * @param type     the entity type.
     * @param renderer the renderer to register.
     * @param <T>      the entity type.
     * @return this.
     */
    @Override
    public <T extends Entity> RenderersRepository register(Class<T> type, Renderer<?> renderer) {
        renderers.put(type, renderer);
        return this;
    }
}
