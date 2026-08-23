package io.sqm.core.walk;

import io.sqm.core.*;

/**
 * Visitor for the pattern-recognition relation, pattern grammar, clause items,
 * options, and scoped expressions.
 *
 * @param <R> visitor result type
 */
public interface PatternRecognitionVisitor<R> {
    /**
     * Visits a pattern-recognition relation.
     *
     * @param table relation being visited
     * @return visitor result
     */
    R visitPatternRecognitionTable(PatternRecognitionTable table);

    /**
     * Visits a pattern measure.
     *
     * @param measure measure being visited
     * @return visitor result
     */
    R visitPatternMeasure(PatternMeasure measure);

    /**
     * Visits a pattern definition.
     *
     * @param definition definition being visited
     * @return visitor result
     */
    R visitPatternDefinition(PatternDefinition definition);

    /**
     * Visits a pattern subset.
     *
     * @param subset subset being visited
     * @return visitor result
     */
    R visitPatternSubset(PatternSubset subset);

    /**
     * Visits rows-per-match behavior.
     *
     * @param rowsPerMatch behavior being visited
     * @return visitor result
     */
    R visitRowsPerMatch(RowsPerMatch rowsPerMatch);

    /**
     * Visits after-match skip behavior.
     *
     * @param afterMatchSkip behavior being visited
     * @return visitor result
     */
    R visitAfterMatchSkip(AfterMatchSkip afterMatchSkip);

    /**
     * Visits a primary pattern variable.
     *
     * @param pattern variable pattern
     * @return visitor result
     */
    R visitPatternVariable(MatchPattern.Variable pattern);

    /**
     * Visits a pattern sequence.
     *
     * @param pattern sequence pattern
     * @return visitor result
     */
    R visitPatternSequence(MatchPattern.Sequence pattern);

    /**
     * Visits a pattern alternation.
     *
     * @param pattern alternation pattern
     * @return visitor result
     */
    R visitPatternAlternation(MatchPattern.Alternation pattern);

    /**
     * Visits a pattern permutation.
     *
     * @param pattern permutation pattern
     * @return visitor result
     */
    R visitPatternPermutation(MatchPattern.Permutation pattern);

    /**
     * Visits a pattern anchor.
     *
     * @param pattern anchor pattern
     * @return visitor result
     */
    R visitPatternAnchor(MatchPattern.Anchor pattern);

    /**
     * Visits the empty pattern.
     *
     * @param pattern empty pattern
     * @return visitor result
     */
    R visitEmptyPattern(MatchPattern.Empty pattern);

    /**
     * Visits a pattern exclusion.
     *
     * @param pattern exclusion pattern
     * @return visitor result
     */
    R visitPatternExclusion(MatchPattern.Exclusion pattern);

    /**
     * Visits a quantified pattern.
     *
     * @param pattern quantified pattern
     * @return visitor result
     */
    R visitQuantifiedPattern(MatchPattern.Quantified pattern);

    /**
     * Visits a pattern-variable column expression.
     *
     * @param expression expression being visited
     * @return visitor result
     */
    R visitPatternColumnExpr(PatternColumnExpr expression);

    /**
     * Visits a classifier expression.
     *
     * @param expression expression being visited
     * @return visitor result
     */
    R visitClassifierExpr(ClassifierExpr expression);

    /**
     * Visits a match-number expression.
     *
     * @param expression expression being visited
     * @return visitor result
     */
    R visitMatchNumberExpr(MatchNumberExpr expression);

    /**
     * Visits a pattern navigation expression.
     *
     * @param expression expression being visited
     * @return visitor result
     */
    R visitPatternNavigationExpr(PatternNavigationExpr expression);

    /**
     * Visits a pattern evaluation expression.
     *
     * @param expression expression being visited
     * @return visitor result
     */
    R visitPatternEvaluationExpr(PatternEvaluationExpr expression);
}
