package io.sqm.render.sqlserver;

import io.sqm.render.spi.RenderersRepository;

/**
 * Factory for SQL Server renderers repository.
 */
public final class Renderers {

    private static final RenderersRepository REPOSITORY = registerDefaults(io.sqm.render.ansi.Renderers.ansiCopy());

    private Renderers() {
    }

    /**
     * Returns a singleton SQL Server renderers repository.
     *
     * @return SQL Server renderers repository.
     */
    public static RenderersRepository sqlServer() {
        return REPOSITORY;
    }

    private static RenderersRepository registerDefaults(RenderersRepository repository) {
        return repository
            .register(new SqlServerFunctionExprRenderer())
            .register(new SqlServerLimitOffsetRenderer())
            .register(new SqlServerSelectQueryRenderer());
    }
}
