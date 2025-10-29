package io.sqm.render.spi;

import io.sqm.core.Node;
import io.sqm.core.repos.HandlersRepository;

public interface RenderersRepository extends HandlersRepository<Renderer<?>> {
    @Override
    <T extends Node> Renderer<T> get(Class<T> type);

    @Override
    @SuppressWarnings("unchecked")
    default <T extends Node> Renderer<T> getFor(T instance) {
        return (Renderer<T>) HandlersRepository.super.getFor(instance);
    }

    @Override
    @SuppressWarnings("unchecked")
    default <T extends Node> Renderer<T> require(Class<T> type) {
        return (Renderer<T>) HandlersRepository.super.require(type);
    }

    @Override
    @SuppressWarnings("unchecked")
    default <T extends Node> Renderer<T> requireFor(T instance) {
        return (Renderer<T>) HandlersRepository.super.requireFor(instance);
    }

    @Override
    RenderersRepository register(Renderer<?> handler);
}
