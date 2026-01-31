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
     * Visits a {@link DateLiteralExpr} node representing a {@code DATE '...'} literal.
     *
     * @param l the date literal expression
     * @return a result produced by the visitor
     */
    R visitDateLiteralExpr(DateLiteralExpr l);

    /**
     * Visits a {@link TimeLiteralExpr} node representing a {@code TIME '...'} literal.
     *
     * @param l the time literal expression
     * @return a result produced by the visitor
     */
    R visitTimeLiteralExpr(TimeLiteralExpr l);

    /**
     * Visits a {@link TimestampLiteralExpr} node representing a {@code TIMESTAMP '...'} literal.
     *
     * @param l the timestamp literal expression
     * @return a result produced by the visitor
     */
    R visitTimestampLiteralExpr(TimestampLiteralExpr l);

    /**
     * Visits an {@link IntervalLiteralExpr} node representing an {@code INTERVAL '...'} literal.
     *
     * @param l the interval literal expression
     * @return a result produced by the visitor
     */
    R visitIntervalLiteralExpr(IntervalLiteralExpr l);

    /**
     * Visits a {@link BitStringLiteralExpr} node representing a bit string literal.
     *
     * @param l the bit string literal expression
     * @return a result produced by the visitor
     */
    R visitBitStringLiteralExpr(BitStringLiteralExpr l);

    /**
     * Visits a {@link HexStringLiteralExpr} node representing a hex string literal.
     *
     * @param l the hex string literal expression
     * @return a result produced by the visitor
     */
    R visitHexStringLiteralExpr(HexStringLiteralExpr l);

    /**
     * Visits a {@link EscapeStringLiteralExpr} node representing a PostgreSQL escape string literal.
     *
     * @param l the escape string literal expression
     * @return a result produced by the visitor
     */
    R visitEscapeStringLiteralExpr(EscapeStringLiteralExpr l);

    /**
     * Visits a {@link DollarStringLiteralExpr} node representing a PostgreSQL dollar-quoted string literal.
     *
     * @param l the dollar-quoted string literal expression
     * @return a result produced by the visitor
     */
    R visitDollarStringLiteralExpr(DollarStringLiteralExpr l);

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

    /**
     * Visits a binary operator expression (e.g. {@code a || b}, {@code x @> y}).
     *
     * @param expr binary operator expression
     * @return visitor result
     */
    R visitBinaryOperatorExpr(BinaryOperatorExpr expr);

    /**
     * Visits a unary operator expression (e.g. {@code -x}, {@code ~x}).
     *
     * @param expr unary operator expression
     * @return visitor result
     */
    R visitUnaryOperatorExpr(UnaryOperatorExpr expr);

    /**
     * Visits a {@link CastExpr}.
     * <p>
     * The visitor is applied recursively to the operand expression.
     * No transformation is performed by this method; it is intended for traversal,
     * analysis, or validation purposes.
     * <p>
     * Subclasses may override this method to implement type-specific behavior
     * (for example, collecting cast targets or validating type usage).
     *
     * @param expr cast expression being visited
     * @return visitor result
     */
    R visitCastExpr(CastExpr expr);

    /**
     * Visits an {@link ArrayExpr}.
     * <p>
     * The visitor is applied recursively to each array element expression.
     * No transformation is performed by this method; it is intended for traversal,
     * analysis, or validation purposes.
     * <p>
     * Subclasses may override this method to implement array-specific behavior
     * (for example, checking element constraints or collecting statistics).
     *
     * @param expr array constructor expression being visited
     * @return visitor result
     */
    R visitArrayExpr(ArrayExpr expr);

    /**
     * Visits an {@link ArraySubscriptExpr}.
     *
     * <p>This method is invoked for expressions that access an element of an array
     * using subscript syntax, such as {@code arr[1]}.</p>
     *
     * <p>Chained subscripts like {@code arr[1][2]} are represented as nested
     * {@link ArraySubscriptExpr} nodes and will result in multiple visits,
     * starting from the outermost expression.</p>
     *
     * <p>This visitor method is purely structural and does not imply any
     * semantic validation, such as index bounds or array dimensionality.
     * Such checks are dialect- or engine-specific and must be handled separately.</p>
     *
     * @param expr the array subscript expression being visited
     * @return the result of visiting the expression
     */
    R visitArraySubscriptExpr(ArraySubscriptExpr expr);

    /**
     * Visits an {@link ArraySliceExpr}.
     *
     * <p>This method is invoked for expressions that slice an array value using
     * syntax like {@code arr[2:5]}.</p>
     *
     * <p>Bounds may be omitted, for example {@code arr[:5]} or {@code arr[2:]}.
     * The meaning of omitted bounds is dialect-specific and is not validated here.</p>
     *
     * @param expr the array slice expression being visited
     * @return the result of visiting the expression
     */
    R visitArraySliceExpr(ArraySliceExpr expr);
}

