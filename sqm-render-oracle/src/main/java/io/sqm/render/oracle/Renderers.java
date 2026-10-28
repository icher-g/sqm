package io.sqm.render.oracle;

import io.sqm.render.spi.RenderersRepository;

import static io.sqm.render.ansi.Renderers.ansiCopy;

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
            repository = registerOracleOverrides(ansiCopy());
        }
        return repository;
    }

    /**
     * Returns a new Oracle renderers repository instance with default registrations.
     *
     * @return isolated Oracle renderers repository.
     */
    public static RenderersRepository oracleCopy() {
        return registerOracleOverrides(ansiCopy());
    }

    private static RenderersRepository registerOracleOverrides(RenderersRepository repository) {
        return repository
            .register(new ModelClauseRenderer())
            .register(new MainModelRenderer())
            .register(new ReferenceModelRenderer())
            .register(new CellSelectorConditionRenderer())
            .register(new CellForRenderer())
            .register(new CellForValuesRenderer())
            .register(new CellForRangeRenderer())
            .register(new ModelAggregateExprRenderer())
            .register(new IterationSpecRenderer())
            .register(new CurrentDimensionExprRenderer())
            .register(new IterationNumberExprRenderer())
            .register(new PreviousModelValueExprRenderer())
            .register(new PresenceValueExprRenderer())
            .register(new CellPresentPredicateRenderer())
            .register(new IsAnyPredicateRenderer())
            .register(new ModelColumnRenderer())
            .register(new ModelRulesRenderer())
            .register(new ModelRuleRenderer())
            .register(new CellTargetRenderer())
            .register(new CellRefExprRenderer())
            .register(new CellAddressRenderer())
            .register(new CellSelectorRenderer())
            .register(new CellSelectorValueRenderer())
            .register(new InsertStatementRenderer())
            .register(new UpdateStatementRenderer())
            .register(new DeleteStatementRenderer())
            .register(new MergeStatementRenderer())
            .register(new MergeClauseRenderer())
            .register(new MergeUpdateActionRenderer())
            .register(new MergeInsertActionRenderer())
            .register(new SelectQueryRenderer())
            .register(new LimitOffsetRenderer())
            .register(new VariableResultTargetRenderer())
            .register(new SequenceValueExprRenderer())
            .register(new PriorExprRenderer())
            .register(new HierarchicalQueryClauseRenderer())
            .register(new TableRenderer())
            .register(new SampledTableRenderer())
            .register(new PatternRecognitionTableRenderer())
            .register(new NamedParamExprRenderer())
            .register(new OrdinalParamExprRenderer());
    }
}
