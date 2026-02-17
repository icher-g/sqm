package io.sqm.core.repos;

import io.sqm.core.Node;

/**
 * An interface of any repository that provides access to handlers.
 *
 * @param <K> an actual type of Handler (SpecParser/Renderer).
 */
public interface HandlersRepository<K extends Handler<?>> {
    /**
     * Returns a Handler for the specific class.
     *
     * @param type the type of the class which handler is needed.
     * @param <T>  the actual type of the entity to be handled.
     * @return a Handler for the required type.
     */
    <T extends Node> K get(Class<T> type);

    /**
     * Returns a Handler for the provided entity.
     *
     * @param instance an instance of the entity that needs to be handled.
     * @param <T>      the actual type of the entity to be handled.
     * @return a Handler.
     */
    default <T extends Node> K getFor(T instance) {
        var key = instance.getTopLevelInterface();
        return get(key);
    }

    /**
     * Returns a Handler if found or throws an exception otherwise.
     *
     * @param type the type of the class which handler is needed.
     * @param <T>  the actual type of the entity to be handled.
     * @return a Handler
     * @throws IllegalArgumentException if no handler found.
     */
    default <T extends Node> K require(Class<T> type) {
        K k = get(type);
        if (k == null) {
            throw new IllegalArgumentException("No handler registered for entity=" + type.getName());
        }
        return k;
    }

    /**
     * Returns a Handler if found or throws an exception otherwise.
     *
     * @param instance an instance of the entity that needs to be handled.
     * @param <T>      the actual type of the entity to be handled.
     * @return a Handler
     * @throws IllegalArgumentException if no handler found.
     */
    default <T extends Node> K requireFor(T instance) {
        var key = instance.getTopLevelInterface();
        return require(key);
    }

    /**
     * Registers a new handler for a type.
     *
     * @param handler the handler implementation
     * @return this.
     */
    HandlersRepository<K> register(K handler);
}