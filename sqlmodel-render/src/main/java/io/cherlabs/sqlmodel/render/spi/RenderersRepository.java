package io.cherlabs.sqlmodel.render.spi;

import io.cherlabs.sqlmodel.core.Entity;
import io.cherlabs.sqlmodel.core.repos.HandlersRepository;
import io.cherlabs.sqlmodel.render.Renderer;

public interface RenderersRepository extends HandlersRepository<Renderer<?>> {
    @Override
    <T extends Entity> Renderer<T> get(Class<T> type);

    @Override
    @SuppressWarnings("unchecked")
    default <T extends Entity> Renderer<T> getFor(T instance) {
        return (Renderer<T>) HandlersRepository.super.getFor(instance);
    }

    @Override
    @SuppressWarnings("unchecked")
    default <T extends Entity> Renderer<T> require(Class<T> type) {
        return (Renderer<T>) HandlersRepository.super.require(type);
    }

    @Override
    @SuppressWarnings("unchecked")
    default <T extends Entity> Renderer<T> requireFor(T instance) {
        return (Renderer<T>) HandlersRepository.super.requireFor(instance);
    }

    @Override
    <T extends Entity> RenderersRepository register(Class<T> type, Renderer<?> handler);
}
