package io.sqm.core.repos;

import io.sqm.core.Entity;

/**
 * An interface of any repository that provides access to handlers.
 * @param <K> an actual type of Handler (SpecParser/Renderer).
 */
public interface HandlersRepository<K extends Handler<?>> {
    /**
     * Returns a Handler for the specific class.
     * @param type the type of the class which handler is needed.
     * @return a Handler for the required type.
     * @param <T> the actual type of the entity to be handled.
     */
    <T extends Entity> K get(Class<T> type);

    /**
     * Returns a Handler for the provided entity.
     * @param instance an instance of the entity that needs to be handled.
     * @return a Handler.
     * @param <T> the actual type of the entity to be handled.
     */
    default <T extends Entity> K getFor(T instance) {
        @SuppressWarnings("unchecked")
        Class<T> t = (Class<T>) instance.getClass();
        return get(t);
    }

    /**
     * Returns a Handler if found or throws an exception otherwise.
     * @param type the type of the class which handler is needed.
     * @return a Handler
     * @param <T> the actual type of the entity to be handled.
     * @exception IllegalArgumentException if no handler found.
     */
    default <T extends Entity> K require(Class<T> type) {
        K k = get(type);
        if (k == null) {
            throw new IllegalArgumentException("No handler registered for entity=" + type.getName());
        }
        return k;
    }

    /**
     * Returns a Handler if found or throws an exception otherwise.
     * @param instance an instance of the entity that needs to be handled.
     * @return a Handler
     * @param <T> the actual type of the entity to be handled.
     * @exception IllegalArgumentException if no handler found.
     */
    default <T extends Entity> K requireFor(T instance) {
        @SuppressWarnings("unchecked")
        Class<T> t = (Class<T>) instance.getClass();
        return require(t);
    }

    /**
     * Registers a new handler for a type.
     * @param handler the handler implementation
     * @return this.
     */
    HandlersRepository<K> register(K handler);
}