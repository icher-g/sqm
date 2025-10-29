package io.sqm.render.repos;

import io.sqm.core.Node;
import io.sqm.render.spi.Renderer;
import io.sqm.render.spi.RenderersRepository;

import java.util.Arrays;
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
    public <T extends Node> Renderer<T> get(Class<T> type) {
        var i = Arrays.stream(type.getInterfaces()).findFirst();
        var p = renderers.get(i.orElse(type));
        if (p != null) {
            return (Renderer<T>) p;
        }
        return null;
    }

    /**
     * Registers a {@link Renderer} for the provided entity type.
     *
     * @param renderer the renderer to register.
     * @return this.
     */
    @Override
    public RenderersRepository register(Renderer<?> renderer) {
        renderers.put(renderer.targetType(), renderer);
        return this;
    }
}
