package io.sqm.render.mysql;

import io.sqm.render.spi.RenderersRepository;

/**
 * Factory for MySQL renderers repository.
 */
public final class Renderers {

    private static RenderersRepository repository;

    private Renderers() {
    }

    /**
     * Returns a singleton MySQL renderers repository.
     *
     * @return MySQL renderers repository.
     */
    public static RenderersRepository mysql() {
        if (repository == null) {
            repository = registerDefaults(io.sqm.render.ansi.Renderers.ansiCopy());
        }
        return repository;
    }

    private static RenderersRepository registerDefaults(RenderersRepository renderersRepository) {
        return renderersRepository
            .register(new MySqlLimitOffsetRenderer())
            .register(new MySqlGroupByRenderer())
            .register(new MySqlRegexPredicateRenderer())
            .register(new MySqlTableRenderer())
            .register(new MySqlSelectQueryRenderer());
    }
}
