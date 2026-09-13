package io.sqm.core.walk;

import io.sqm.core.*;

/**
 * Visitor for multidimensional MODEL declarations, cells, and rules.
 * @param <R> result type
 */
public interface ModelVisitor<R> {
    /**
     * Visits a {@link ModelColumn}.
     * @param node node being visited
     * @return visitor result
     */
    R visitModelColumn(ModelColumn node);

    /**
     * Visits a {@link CellRefExpr}.
     * @param node node being visited
     * @return visitor result
     */
    R visitCellRefExpr(CellRefExpr node);

    /**
     * Visits a {@link CellTarget}.
     * @param node node being visited
     * @return visitor result
     */
    R visitCellTarget(CellTarget node);

    /**
     * Visits a {@link ModelAggregateExpr}.
     * @param node node being visited
     * @return visitor result
     */
    R visitModelAggregateExpr(ModelAggregateExpr node);

    /**
     * Visits a {@link CurrentDimensionExpr}.
     * @param node node being visited
     * @return visitor result
     */
    R visitCurrentDimensionExpr(CurrentDimensionExpr node);

    /**
     * Visits a {@link IterationNumberExpr}.
     * @param node node being visited
     * @return visitor result
     */
    R visitIterationNumberExpr(IterationNumberExpr node);

    /**
     * Visits a {@link PreviousModelValueExpr}.
     * @param node node being visited
     * @return visitor result
     */
    R visitPreviousModelValueExpr(PreviousModelValueExpr node);

    /**
     * Visits a {@link PresenceValueExpr}.
     * @param node node being visited
     * @return visitor result
     */
    R visitPresenceValueExpr(PresenceValueExpr node);

    /**
     * Visits a {@link CellPresentPredicate}.
     * @param node node being visited
     * @return visitor result
     */
    R visitCellPresentPredicate(CellPresentPredicate node);

    /**
     * Visits a {@link IterationSpec}.
     * @param node node being visited
     * @return visitor result
     */
    R visitIterationSpec(IterationSpec node);

    /**
     * Visits a {@link ModelRule}.
     * @param node node being visited
     * @return visitor result
     */
    R visitModelRule(ModelRule node);

    /**
     * Visits a {@link ModelRules}.
     * @param node node being visited
     * @return visitor result
     */
    R visitModelRules(ModelRules node);

    /**
     * Visits a {@link MainModel}.
     * @param node node being visited
     * @return visitor result
     */
    R visitMainModel(MainModel node);

    /**
     * Visits a {@link ReferenceModel}.
     * @param node node being visited
     * @return visitor result
     */
    R visitReferenceModel(ReferenceModel node);

    /**
     * Visits a {@link ModelClause}.
     * @param node node being visited
     * @return visitor result
     */
    R visitModelClause(ModelClause node);

    /**
     * Visits a {@link CellSelector.Value}.
     * @param node node being visited
     * @return visitor result
     */
    R visitCellSelectorValue(CellSelector.Value node);

    /**
     * Visits a {@link CellSelector.Condition}.
     * @param node node being visited
     * @return visitor result
     */
    R visitCellSelectorCondition(CellSelector.Condition node);

    /**
     * Visits a MODEL wildcard predicate.
     *
     * @param node wildcard predicate
     * @return visitor result
     */
    R visitIsAnyPredicate(IsAnyPredicate node);

    /**
     * Visits a {@link CellFor.Values}.
     * @param node node being visited
     * @return visitor result
     */
    R visitCellForValues(CellFor.Values node);


    /**
     * Visits a {@link CellFor.Range}.
     * @param node node being visited
     * @return visitor result
     */
    R visitCellForRange(CellFor.Range node);

}
