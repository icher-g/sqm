package io.cherlabs.sqlmodel.core.repos;

import io.cherlabs.sqlmodel.core.Entity;

public interface HandlersRepository<K extends Handler<?>> {
    <T extends Entity> K get(Class<T> type);

    default <T extends Entity> K getFor(T instance) {
        @SuppressWarnings("unchecked")
        Class<T> t = (Class<T>) instance.getClass();
        return get(t);
    }

    default <T extends Entity> K require(Class<T> type) {
        K k = get(type);
        if (k == null) {
            throw new IllegalArgumentException("No handler registered for entity=" + type.getName());
        }
        return k;
    }

    default <T extends Entity> K requireFor(T instance) {
        @SuppressWarnings("unchecked")
        Class<T> t = (Class<T>) instance.getClass();
        return require(t);
    }

    <T extends Entity> HandlersRepository<K> register(Class<T> type, K handler);
}