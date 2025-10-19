package io.sqm.core.repos;

import io.sqm.core.Entity;

/**
 * Base marker for all handler kinds (Renderer, Parser, …)
 *
 * @param <T> an entity type.
 */
public interface Handler<T extends Entity> {
    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    Class<T> targetType();
}
