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
            .register(new CteDefRenderer())
            .register(new OrderItemRenderer())
            .register(new FunctionTableRenderer())
            .register(new DistinctSpecRenderer())
            .register(new CastExprRenderer())
            .register(new LateralRenderer())
            .register(new LockingClauseRenderer())
            .register(new RegexPredicateRenderer())
            .register(new ArrayExprRenderer())
            .register(new ArraySubscriptExprRenderer())
            .register(new ArraySliceExprRenderer());
    }
}
