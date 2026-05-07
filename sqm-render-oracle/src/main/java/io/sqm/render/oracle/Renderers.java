package io.sqm.render.oracle;

import io.sqm.render.spi.RenderersRepository;

/**
 * Factory for Oracle renderers repository.
 */
public final class Renderers {
    private static RenderersRepository repository;

    private Renderers() {
    }

    /**
     * Returns a singleton Oracle renderers repository.
     *
     * @return Oracle renderers repository.
     */
    public static RenderersRepository oracle() {
        if (repository == null) {
            repository = io.sqm.render.ansi.Renderers.ansiCopy();
        }
        return repository;
    }

    /**
     * Returns a new Oracle renderers repository instance with default registrations.
     *
     * @return isolated Oracle renderers repository.
     */
    public static RenderersRepository oracleCopy() {
        return io.sqm.render.ansi.Renderers.ansiCopy();
    }
}
