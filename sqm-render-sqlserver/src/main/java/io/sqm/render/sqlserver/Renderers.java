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
            .register(new VariableTableRenderer())
            .register(new OutputColumnExprRenderer())
            .register(new OutputStarResultItemRenderer())
            .register(new FunctionExprRenderer())
            .register(new SequenceValueExprRenderer())
            .register(new PivotMeasureRenderer())
            .register(new PivotValueRenderer())
            .register(new UnpivotInputRenderer())
            .register(new UnpivotTableRenderer())
            .register(new LimitOffsetRenderer())
            .register(new CrossJoinRenderer())
            .register(new OnJoinRenderer())
            .register(new SelectQueryRenderer())
            .register(new ResultClauseRenderer())
            .register(new RelationResultTargetRenderer());
    }
}
