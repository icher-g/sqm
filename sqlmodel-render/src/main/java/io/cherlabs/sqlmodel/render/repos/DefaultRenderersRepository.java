package io.cherlabs.sqlmodel.render.repos;

import io.cherlabs.sqlmodel.core.*;
import io.cherlabs.sqlmodel.render.Renderer;
import io.cherlabs.sqlmodel.render.spi.RenderersRepository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class DefaultRenderersRepository implements RenderersRepository {

    private final Map<Class<?>, Renderer<?>> renderers = new ConcurrentHashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Entity> Renderer<T> get(Class<T> type) {
        var p = renderers.get(type);
        if (p != null) {
            return (Renderer<T>) p;
        }
        return null;
    }

    @Override
    public <T extends Entity> RenderersRepository register(Class<T> type, Renderer<?> renderer) {
        renderers.put(type, renderer);
        return this;
    }
}
