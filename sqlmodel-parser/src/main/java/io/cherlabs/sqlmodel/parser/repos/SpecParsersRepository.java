package io.cherlabs.sqlmodel.parser.repos;

import io.cherlabs.sqlmodel.core.Entity;
import io.cherlabs.sqlmodel.core.repos.HandlersRepository;
import io.cherlabs.sqlmodel.parser.SpecParser;

public interface SpecParsersRepository extends HandlersRepository<SpecParser<?>> {
    @Override
    <T extends Entity> SpecParser<T> get(Class<T> type);

    @Override
    @SuppressWarnings("unchecked")
    default <T extends Entity> SpecParser<T> getFor(T instance) {
        return (SpecParser<T>) HandlersRepository.super.getFor(instance);
    }

    @Override
    @SuppressWarnings("unchecked")
    default <T extends Entity> SpecParser<T> require(Class<T> type) {
        return (SpecParser<T>) HandlersRepository.super.require(type);
    }

    @Override
    @SuppressWarnings("unchecked")
    default <T extends Entity> SpecParser<T> requireFor(T instance) {
        return (SpecParser<T>) HandlersRepository.super.requireFor(instance);
    }

    @Override
    <T extends Entity> SpecParsersRepository register(Class<T> type, SpecParser<?> handler);
}
