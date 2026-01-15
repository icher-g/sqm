package io.sqm.core.walk;

import io.sqm.core.*;

/**
 * Visitor for traversing all {@link Predicate} nodes in the SQM model.
 * <p>
 * Provides type-specific callbacks for each logical or comparison predicate
 * used in SQL boolean expressions. Enables type-safe processing without
 * explicit {@code instanceof} checks.
 *
 * @param <R> the result type produced by the visitor
 */
public interface PredicateVisitor<R> {

    /**
     * Visits an {@link AnyAllPredicate}, representing
     * {@code <expr> = ANY(<subquery>)} or {@code <expr> > ALL(<subquery>)} constructs.
     *
     * @param p the predicate being visited
     * @return a result produced by the visitor
     */
    R visitAnyAllPredicate(AnyAllPredicate p);

    /**
     * Visits a {@link BetweenPredicate}, representing a {@code BETWEEN} test.
     *
     * @param p the predicate being visited
     * @return a result produced by the visitor
     */
    R visitBetweenPredicate(BetweenPredicate p);

    /**
     * Visits a {@link ComparisonPredicate}, representing a comparison operator such as
     * {@code =}, {@code <>}, {@code <}, {@code >}, {@code <=}, or {@code >=}.
     *
     * @param p the predicate being visited
     * @return a result produced by the visitor
     */
    R visitComparisonPredicate(ComparisonPredicate p);

    /**
     * Visits an {@link ExistsPredicate}, representing an {@code EXISTS(subquery)} test.
     *
     * @param p the predicate being visited
     * @return a result produced by the visitor
     */
    R visitExistsPredicate(ExistsPredicate p);

    /**
     * Visits an {@link InPredicate}, representing an {@code IN(...)} test.
     *
     * @param p the predicate being visited
     * @return a result produced by the visitor
     */
    R visitInPredicate(InPredicate p);

    /**
     * Visits an {@link IsNullPredicate}, representing an {@code IS [NOT] NULL} test.
     *
     * @param p the predicate being visited
     * @return a result produced by the visitor
     */
    R visitIsNullPredicate(IsNullPredicate p);

    /**
     * Visits a {@link LikePredicate}, representing a {@code LIKE} or {@code NOT LIKE} pattern match.
     *
     * @param p the predicate being visited
     * @return a result produced by the visitor
     */
    R visitLikePredicate(LikePredicate p);

    /**
     * Visits a {@link NotPredicate}, representing logical negation ({@code NOT ...}).
     *
     * @param p the predicate being visited
     * @return a result produced by the visitor
     */
    R visitNotPredicate(NotPredicate p);

    /**
     * Visits a {@link UnaryPredicate}, representing a single-operand predicate such as
     * {@code EXISTS}, {@code IS NULL}, or other unary forms.
     *
     * @param p the predicate being visited
     * @return a result produced by the visitor
     */
    R visitUnaryPredicate(UnaryPredicate p);

    /**
     * Visits an {@link AndPredicate}, representing a logical conjunction ({@code AND}).
     *
     * @param p the predicate being visited
     * @return a result produced by the visitor
     */
    R visitAndPredicate(AndPredicate p);

    /**
     * Visits an {@link OrPredicate}, representing a logical disjunction ({@code OR}).
     *
     * @param p the predicate being visited
     * @return a result produced by the visitor
     */
    R visitOrPredicate(OrPredicate p);

    /**
     * Visits an {@link ExprPredicate}.
     * <p>
     * The visitor is applied recursively to the wrapped expression.
     * No transformation is performed by this method; it is intended for traversal,
     * analysis, or validation purposes.
     * <p>
     * Subclasses may override this method to implement expression-predicate-specific behavior
     * (for example, validating that the wrapped expression is boolean-valued in a given dialect).
     *
     * @param predicate expression predicate being visited
     * @return a result produced by the visitor
     */
    R visitExprPredicate(ExprPredicate predicate);
}

