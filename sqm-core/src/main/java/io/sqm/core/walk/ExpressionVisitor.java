package io.sqm.core.walk;

import io.sqm.core.*;

/**
 * Visitor for traversing all {@link Expression} nodes in the SQM model.
 * <p>
 * This interface provides type-specific callbacks for every concrete expression
 * variant. It enables type-safe processing of SQL expression trees without
 * explicit {@code instanceof} checks.
 *
 * @param <R> the result type produced by the visitor
 */
public interface ExpressionVisitor<R> {

    /**
     * Visits a {@link CaseExpr} node representing a {@code CASE WHEN ... THEN ... END} expression.
     *
     * @param c the case expression
     * @return a result produced by the visitor
     */
    R visitCaseExpr(CaseExpr c);

    /**
     * Visits a {@link ColumnExpr} node referencing a table column.
     *
     * @param c the column expression
     * @return a result produced by the visitor
     */
    R visitColumnExpr(ColumnExpr c);

    /**
     * Visits a {@link FunctionExpr} node representing a function call.
     *
     * @param f the function expression
     * @return a result produced by the visitor
     */
    R visitFunctionExpr(FunctionExpr f);

    /**
     * Visits a {@link FunctionExpr.Arg} node representing a single argument of a function call.
     *
     * @param a the function argument
     * @return a result produced by the visitor
     */
    R visitFunctionArgExpr(FunctionExpr.Arg a);

    /**
     * Visits an {@link AnonymousParamExpr}, representing an anonymous positional
     * parameter such as {@code ?}.
     *
     * @param p the anonymous parameter expression
     * @return the result of the visit
     */
    R visitAnonymousParamExpr(AnonymousParamExpr p);

    /**
     * Visits a {@link NamedParamExpr}, representing a parameter identified by a
     * canonical name such as {@code :id} or {@code @tenant}.
     *
     * @param p the named parameter expression
     * @return the result of the visit
     */
    R visitNamedParamExpr(NamedParamExpr p);

    /**
     * Visits an {@link OrdinalParamExpr}, representing a positional parameter
     * with an explicit index such as {@code $1} or {@code ?2}.
     *
     * @param p the ordinal parameter expression
     * @return the result of the visit
     */
    R visitOrdinalParamExpr(OrdinalParamExpr p);

    /**
     * Visits a {@link LiteralExpr} node representing a literal value.
     *
     * @param l the literal expression
     * @return a result produced by the visitor
     */
    R visitLiteralExpr(LiteralExpr l);

    /**
     * Visits a {@link RowExpr} node representing a row value constructor
     * such as {@code (a, b, c)}.
     *
     * @param v the row expression
     * @return a result produced by the visitor
     */
    R visitRowExpr(RowExpr v);

    /**
     * Visits a {@link QueryExpr} node representing a scalar subquery
     * used as an expression.
     *
     * @param v the query expression
     * @return a result produced by the visitor
     */
    R visitQueryExpr(QueryExpr v);

    /**
     * Visits a {@link RowListExpr} node representing a list of row expressions,
     * for example {@code ((1,2),(3,4))} used in {@code IN} predicates or value sets.
     *
     * @param v the row list expression
     * @return a result produced by the visitor
     */
    R visitRowListExpr(RowListExpr v);
}

