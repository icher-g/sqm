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

    /**
     * Visits an {@link AddArithmeticExpr} node.
     *
     * <p>This method is invoked when the visitor encounters a binary addition
     * expression of the form {@code lhs + rhs}. Implementations may perform
     * processing, transformation, or traversal of the node and its operands.</p>
     *
     * @param expr the addition expression being visited, never {@code null}
     * @return a visitor-defined result
     */
    R visitAddArithmeticExpr(AddArithmeticExpr expr);

    /**
     * Visits a {@link SubArithmeticExpr} node.
     *
     * <p>This method is invoked when the visitor encounters a binary subtraction
     * expression of the form {@code lhs - rhs}. Implementations may perform
     * processing, transformation, or traversal of the node and its operands.</p>
     *
     * @param expr the subtraction expression being visited, never {@code null}
     * @return a visitor-defined result
     */
    R visitSubArithmeticExpr(SubArithmeticExpr expr);

    /**
     * Visits a {@link MulArithmeticExpr} node.
     *
     * <p>This method is invoked when the visitor encounters a binary multiplication
     * expression of the form {@code lhs * rhs}. Implementations may perform
     * processing, transformation, or traversal of the node and its operands.</p>
     *
     * @param expr the multiplication expression being visited, never {@code null}
     * @return a visitor-defined result
     */
    R visitMulArithmeticExpr(MulArithmeticExpr expr);

    /**
     * Visits a {@link DivArithmeticExpr} node.
     *
     * <p>This method is invoked when the visitor encounters a binary division
     * expression of the form {@code lhs / rhs}. Implementations may perform
     * processing, transformation, or traversal of the node and its operands.</p>
     *
     * @param expr the division expression being visited, never {@code null}
     * @return a visitor-defined result
     */
    R visitDivArithmeticExpr(DivArithmeticExpr expr);

    /**
     * Visits a {@link ModArithmeticExpr} node.
     *
     * <p>This method is invoked when the visitor encounters a binary modulo
     * expression of the form {@code lhs % rhs}. Implementations may perform
     * processing, transformation, or traversal of the node and its operands.
     * Note that the rendered SQL form of the modulo operator may vary by dialect.</p>
     *
     * @param expr the modulo expression being visited, never {@code null}
     * @return a visitor-defined result
     */
    R visitModArithmeticExpr(ModArithmeticExpr expr);

    /**
     * Visits a {@link NegativeArithmeticExpr} node.
     *
     * <p>This method is invoked when the visitor encounters a unary negation
     * expression of the form {@code -expr}. Implementations may perform
     * processing, transformation, or traversal of the negated operand.</p>
     *
     * @param expr the negation expression being visited, never {@code null}
     * @return a visitor-defined result
     */
    R visitNegativeArithmeticExpr(NegativeArithmeticExpr expr);
}

