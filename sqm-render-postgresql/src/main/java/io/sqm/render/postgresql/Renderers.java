package io.sqm.render.postgresql;

import io.sqm.render.spi.RenderersRepository;

public final class Renderers {

    private static RenderersRepository repository;

    private Renderers() {
    }

    public static RenderersRepository postgres() {
        if (repository == null) {
            repository = registerDefaults(io.sqm.render.ansi.Renderers.ansi());
        }
        return repository;
    }

    private static RenderersRepository registerDefaults(RenderersRepository r) {
        return r
            .register(new CastExprRenderer());
    }
}
