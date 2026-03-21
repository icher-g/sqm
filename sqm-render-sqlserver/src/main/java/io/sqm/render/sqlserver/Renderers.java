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
            .register(new InsertStatementRenderer())
            .register(new UpdateStatementRenderer())
            .register(new DeleteStatementRenderer())
            .register(new MergeStatementRenderer())
            .register(new MergeClauseRenderer())
            .register(new MergeUpdateActionRenderer())
            .register(new MergeDeleteActionRenderer())
            .register(new MergeDoNothingActionRenderer())
            .register(new MergeInsertActionRenderer())
            .register(new TableRenderer())
            .register(new VariableTableRefRenderer())
            .register(new OutputColumnExprRenderer())
            .register(new OutputStarResultItemRenderer())
            .register(new FunctionExprRenderer())
            .register(new LimitOffsetRenderer())
            .register(new SelectQueryRenderer())
            .register(new ResultClauseRenderer())
            .register(new ResultIntoRenderer());
    }
}
