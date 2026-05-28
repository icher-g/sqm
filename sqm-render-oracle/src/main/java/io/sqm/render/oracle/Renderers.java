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
            repository = registerOracleOverrides(io.sqm.render.ansi.Renderers.ansiCopy());
        }
        return repository;
    }

    /**
     * Returns a new Oracle renderers repository instance with default registrations.
     *
     * @return isolated Oracle renderers repository.
     */
    public static RenderersRepository oracleCopy() {
        return registerOracleOverrides(io.sqm.render.ansi.Renderers.ansiCopy());
    }

    private static RenderersRepository registerOracleOverrides(RenderersRepository repository) {
        return repository
            .register(new InsertStatementRenderer())
            .register(new UpdateStatementRenderer())
            .register(new DeleteStatementRenderer())
            .register(new MergeStatementRenderer())
            .register(new MergeClauseRenderer())
            .register(new MergeUpdateActionRenderer())
            .register(new MergeInsertActionRenderer())
            .register(new LimitOffsetRenderer())
            .register(new VariableResultTargetRenderer())
            .register(new SequenceValueExprRenderer())
            .register(new PriorExprRenderer())
            .register(new HierarchicalQueryClauseRenderer())
            .register(new TableRenderer())
            .register(new SampledTableRenderer())
            .register(new NamedParamExprRenderer())
            .register(new OrdinalParamExprRenderer());
    }
}
