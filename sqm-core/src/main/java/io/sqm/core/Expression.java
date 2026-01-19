package io.sqm.core;

import io.sqm.core.internal.FuncStarArg;
import io.sqm.core.internal.FunctionArgExpr;
import io.sqm.core.match.ExpressionMatch;

import java.util.Arrays;
import java.util.List;

/**
 * Any value-producing node (scalar or boolean).
 */
public sealed interface Expression extends Node
    permits ArithmeticExpr, ArrayExpr, BinaryOperatorExpr, CaseExpr, CastExpr, ColumnExpr, DialectExpression, FunctionExpr, FunctionExpr.Arg, LiteralExpr, ParamExpr, Predicate, UnaryOperatorExpr, ValueSet {

    /**
     * Creates a literal expression.
     *
     * @param value a value.
     * @return A newly created instance of a literal expression.
     */
    static LiteralExpr literal(Object value) {
        return LiteralExpr.of(value);
    }

    /**
     * Creates a column reference expression.
     *
     * @param name a name of the column.
     * @return A newly created instance of the column reference.
     */
    static ColumnExpr column(String name) {
        return ColumnExpr.of(name);
    }

    /**
     * Creates a column reference expression.
     *
     * @param name  a name of the column.
     * @param table a table this column belongs to.
     * @return A newly created instance of the column reference.
     */
    static ColumnExpr column(String table, String name) {
        return ColumnExpr.of(table, name);
    }

    /**
     * Creates a function call expression.
     *
     * @param name a function name
     * @param args an array of function arguments.
     * @return A newly created instance of a function call expression.
     */
    static FunctionExpr func(String name, FunctionExpr.Arg... args) {
        return FunctionExpr.of(name, args);
    }

    /**
     * Creates a function argument that wraps an expression.
     *
     * @param expr an expression to wrap.
     * @return A newly created instance of a function argument.
     */
    static FunctionExpr.Arg.ExprArg funcArg(Expression expr) {
        return new FunctionArgExpr(expr);
    }

    /**
     * Creates a function argument that wraps a '*'.
     *
     * @return A newly created instance of a function argument.
     */
    static FunctionExpr.Arg.StarArg starArg() {
        return new FuncStarArg();
    }

    /**
     * Creates an instance of CaseExpr.
     *
     * @param whens an array of WHEN...THEN statements.
     * @return a newly created instance.
     */
    static CaseExpr kase(WhenThen... whens) {
        return CaseExpr.of(List.of(whens));
    }

    /**
     * Creates an instance of CaseExpr.
     *
     * @param whens a list of WHEN...THEN statements.
     * @return a newly created instance.
     */
    static CaseExpr kase(List<WhenThen> whens) {
        return CaseExpr.of(whens);
    }

    /**
     * Creates a new instance of a query expression.
     *
     * @param subquery a sub query to wrap.
     * @return A newly created instance of a wrapped query.
     */
    static QueryExpr subquery(Query subquery) {
        return QueryExpr.of(subquery);
    }

    /**
     * Creates a list of scalar expressions. {@code (a,b)}
     *
     * @param items a list of scalars.
     * @return A newly created instance of the {@link RowExpr}.
     */
    static RowExpr row(List<Expression> items) {
        return RowExpr.of(items);
    }

    /**
     * Creates a list of scalars. {@code (a,b)}
     *
     * @param items a list of scalars.
     * @return A newly created instance of the {@link RowExpr}.
     */
    static RowExpr row(Object... items) {
        return RowExpr.of(Arrays.stream(items).map(i -> (Expression) literal(i)).toList());
    }

    /**
     * Creates a list of rows expressions. {@code (a,b) IN ((1,2), (3,4))}
     *
     * @param rows a list of expressions.
     * @return A newly created instance of the {@link RowListExpr}.
     */
    static RowListExpr rows(List<RowExpr> rows) {
        return RowListExpr.of(rows);
    }

    /**
     * Creates a list of tuples.
     * For example: {@code WHERE (c1, c2) IN ((1, 2), (3, 4))}
     *
     * @param rows a list of tuples.
     * @return Values that represents a list of tuples.
     */
    static RowListExpr rows(RowExpr... rows) {
        return RowListExpr.of(List.of(rows));
    }

    /**
     * Creates a SELECT item based on the current expression and provided alias.
     * <p>Example:</p>
     * <pre>
     *     {@code
     *     // with DSL usage.
     *     select(col("u", "id").as("userId"), col("u", "name").toSelectItem());
     *     }
     * </pre>
     *
     * @param alias an alias.
     * @return {@link SelectItem}.
     */
    default SelectItem as(String alias) {
        return SelectItem.expr(this).as(alias);
    }

    /**
     * Creates a SELECT item based on the current expression.
     * <p>Example:</p>
     * <pre>
     *     {@code
     *     // with DSL usage.
     *     select(col("u", "id").as("userId"), col("u", "name").toSelectItem());
     *     }
     * </pre>
     *
     * @return {@link SelectItem}.
     */
    default SelectItem toSelectItem() {
        return SelectItem.expr(this).as(null);
    }

    /**
     * Creates an arithmetic addition expression of the form {@code this + rhs}.
     *
     * <p>The returned expression represents SQL addition and is equivalent to
     * applying the {@link AddArithmeticExpr} operator to the current expression
     * as the left-hand side operand.</p>
     *
     * @param rhs the right-hand side operand, must not be {@code null}
     * @return a new {@link AddArithmeticExpr} representing {@code this + rhs}
     */
    default AddArithmeticExpr add(Expression rhs) {
        return AddArithmeticExpr.of(this, rhs);
    }

    /**
     * Creates an arithmetic subtraction expression of the form {@code this - rhs}.
     *
     * <p>The returned expression represents SQL subtraction and is equivalent to
     * applying the {@link SubArithmeticExpr} operator to the current expression
     * as the left-hand side operand.</p>
     *
     * @param rhs the right-hand side operand, must not be {@code null}
     * @return a new {@link SubArithmeticExpr} representing {@code this - rhs}
     */
    default SubArithmeticExpr sub(Expression rhs) {
        return SubArithmeticExpr.of(this, rhs);
    }

    /**
     * Creates an arithmetic multiplication expression of the form {@code this * rhs}.
     *
     * <p>The returned expression represents SQL multiplication and is equivalent to
     * applying the {@link MulArithmeticExpr} operator to the current expression
     * as the left-hand side operand.</p>
     *
     * @param rhs the right-hand side operand, must not be {@code null}
     * @return a new {@link MulArithmeticExpr} representing {@code this * rhs}
     */
    default MulArithmeticExpr mul(Expression rhs) {
        return MulArithmeticExpr.of(this, rhs);
    }

    /**
     * Creates an arithmetic division expression of the form {@code this / rhs}.
     *
     * <p>The returned expression represents SQL division and is equivalent to
     * applying the {@link DivArithmeticExpr} operator to the current expression
     * as the left-hand side operand.</p>
     *
     * @param rhs the right-hand side operand, must not be {@code null}
     * @return a new {@link DivArithmeticExpr} representing {@code this / rhs}
     */
    default DivArithmeticExpr div(Expression rhs) {
        return DivArithmeticExpr.of(this, rhs);
    }

    /**
     * Creates an arithmetic modulo expression of the form {@code this % rhs}.
     *
     * <p>The exact SQL representation of the modulo operator may depend on the
     * rendering dialect, but the logical structure remains {@code lhs % rhs}.</p>
     *
     * @param rhs the right-hand side operand, must not be {@code null}
     * @return a new {@link ModArithmeticExpr} representing {@code this % rhs}
     */
    default ModArithmeticExpr mod(Expression rhs) {
        return ModArithmeticExpr.of(this, rhs);
    }

    /**
     * Creates a unary negation expression of the form {@code -this}.
     *
     * <p>This corresponds to numeric negation in SQL and produces a
     * {@link NegativeArithmeticExpr} that negates the current expression.</p>
     *
     * @return a new {@link NegativeArithmeticExpr} representing {@code -this}
     */
    default NegativeArithmeticExpr neg() {
        return NegativeArithmeticExpr.of(this);
    }

    /**
     * Creates a comparison predicate with {@link ComparisonOperator#EQ} between current expression and the provided one.
     *
     * @param other an expression to compare to.
     * @return A newly created instance of a comparison predicate.
     */
    default ComparisonPredicate eq(Expression other) {
        return ComparisonPredicate.of(this, ComparisonOperator.EQ, other);
    }

    /**
     * Creates a comparison predicate with {@link ComparisonOperator#EQ} between current expression and the provided one.
     *
     * @param scalar a scalar to compare to.
     * @return A newly created instance of a comparison predicate.
     */
    default ComparisonPredicate eq(Object scalar) {
        return ComparisonPredicate.of(this, ComparisonOperator.EQ, Expression.literal(scalar));
    }

    /**
     * Creates a comparison predicate with {@link ComparisonOperator#NE} between current expression and the provided one.
     *
     * @param other an expression to compare to.
     * @return A newly created instance of a comparison predicate.
     */
    default ComparisonPredicate ne(Expression other) {
        return ComparisonPredicate.of(this, ComparisonOperator.NE, other);
    }

    /**
     * Creates a comparison predicate with {@link ComparisonOperator#NE} between current expression and the provided one.
     *
     * @param scalar a scalar to compare to.
     * @return A newly created instance of a comparison predicate.
     */
    default ComparisonPredicate ne(Object scalar) {
        return ComparisonPredicate.of(this, ComparisonOperator.NE, Expression.literal(scalar));
    }

    /**
     * Creates a comparison predicate with {@link ComparisonOperator#LT} between current expression and the provided one.
     *
     * @param other an expression to compare to.
     * @return A newly created instance of a comparison predicate.
     */
    default ComparisonPredicate lt(Expression other) {
        return ComparisonPredicate.of(this, ComparisonOperator.LT, other);
    }

    /**
     * Creates a comparison predicate with {@link ComparisonOperator#LT} between current expression and the provided one.
     *
     * @param scalar a scalar to compare to.
     * @return A newly created instance of a comparison predicate.
     */
    default ComparisonPredicate lt(Object scalar) {
        return ComparisonPredicate.of(this, ComparisonOperator.LT, Expression.literal(scalar));
    }

    /**
     * Creates a comparison predicate with {@link ComparisonOperator#LTE} between current expression and the provided one.
     *
     * @param other an expression to compare to.
     * @return A newly created instance of a comparison predicate.
     */
    default ComparisonPredicate lte(Expression other) {
        return ComparisonPredicate.of(this, ComparisonOperator.LTE, other);
    }

    /**
     * Creates a comparison predicate with {@link ComparisonOperator#LTE} between current expression and the provided one.
     *
     * @param scalar a scalar to compare to.
     * @return A newly created instance of a comparison predicate.
     */
    default ComparisonPredicate lte(Object scalar) {
        return ComparisonPredicate.of(this, ComparisonOperator.LTE, Expression.literal(scalar));
    }

    /**
     * Creates a comparison predicate with {@link ComparisonOperator#GT} between current expression and the provided one.
     *
     * @param other an expression to compare to.
     * @return A newly created instance of a comparison predicate.
     */
    default ComparisonPredicate gt(Expression other) {
        return ComparisonPredicate.of(this, ComparisonOperator.GT, other);
    }

    /**
     * Creates a comparison predicate with {@link ComparisonOperator#GT} between current expression and the provided one.
     *
     * @param scalar a scalar to compare to.
     * @return A newly created instance of a comparison predicate.
     */
    default ComparisonPredicate gt(Object scalar) {
        return ComparisonPredicate.of(this, ComparisonOperator.GT, Expression.literal(scalar));
    }

    /**
     * Creates a comparison predicate with {@link ComparisonOperator#GTE} between current expression and the provided one.
     *
     * @param other an expression to compare to.
     * @return A newly created instance of a comparison predicate.
     */
    default ComparisonPredicate gte(Expression other) {
        return ComparisonPredicate.of(this, ComparisonOperator.GTE, other);
    }

    /**
     * Creates a comparison predicate with {@link ComparisonOperator#GTE} between current expression and the provided one.
     *
     * @param scalar a scalar to compare to.
     * @return A newly created instance of a comparison predicate.
     */
    default ComparisonPredicate gte(Object scalar) {
        return ComparisonPredicate.of(this, ComparisonOperator.GTE, literal(scalar));
    }

    /**
     * Creates IN predicate for the current expression.
     *
     * @param values a set of values to look in.
     * @return A newly created instance of IN predicate.
     */
    default InPredicate in(ValueSet values) {
        return InPredicate.of(this, values, false);
    }

    /**
     * Creates IN predicate for the current expression.
     *
     * @param values a set of values to look in.
     * @return A newly created instance of IN predicate.
     */
    default InPredicate in(Object... values) {
        return InPredicate.of(this, row(values), false);
    }

    /**
     * Creates NOT IN predicate for the current expression.
     *
     * @param values a set of values to look not in.
     * @return A newly created instance of NOT IN predicate.
     */
    default InPredicate notIn(ValueSet values) {
        return InPredicate.of(this, values, true);
    }

    /**
     * Creates NOT IN predicate for the current expression.
     *
     * @param values a set of values to look not in.
     * @return A newly created instance of NOT IN predicate.
     */
    default InPredicate notIn(Object... values) {
        return InPredicate.of(this, row(values), true);
    }

    /**
     * Creates BETWEEN predicate for the current expression.
     *
     * @param lower a lower part of the predicate.
     * @param upper an upper part of the predicate.
     * @return A newly created instance of the BETWEEN predicate.
     */
    default BetweenPredicate between(Expression lower, Expression upper) {
        return BetweenPredicate.of(this, lower, upper, false, false);
    }

    /**
     * Creates BETWEEN predicate for the current expression.
     *
     * @param lower a lower part of the predicate.
     * @param upper an upper part of the predicate.
     * @return A newly created instance of the BETWEEN predicate.
     */
    default BetweenPredicate between(Object lower, Object upper) {
        return BetweenPredicate.of(this, literal(lower), literal(upper), false, false);
    }

    /**
     * Creates LIKE predicate for the current expression.
     *
     * @param pattern a pattern to use in LIKE predicate.
     * @return A newly created instance of the LIKE predicate.
     */
    default LikePredicate like(Expression pattern) {
        return LikePredicate.of(this, pattern, false);
    }

    /**
     * Creates LIKE predicate for the current expression.
     *
     * @param pattern a pattern to use in LIKE predicate.
     * @return A newly created instance of the LIKE predicate.
     */
    default LikePredicate like(String pattern) {
        return LikePredicate.of(this, literal(pattern), false);
    }

    /**
     * Creates NOT LIKE predicate for the current expression.
     *
     * @param pattern a pattern to use in NOT LIKE predicate.
     * @return A newly created instance of the NOT LIKE predicate.
     */
    default LikePredicate notLike(Expression pattern) {
        return LikePredicate.of(this, pattern, true);
    }

    /**
     * Creates NOT LIKE predicate for the current expression.
     *
     * @param pattern a pattern to use in NOT LIKE predicate.
     * @return A newly created instance of the NOT LIKE predicate.
     */
    default LikePredicate notLike(String pattern) {
        return LikePredicate.of(this, literal(pattern), true);
    }

    /**
     * Creates ILIKE predicate for the current expression.
     *
     * @param pattern a pattern to use in ILIKE predicate.
     * @return A newly created instance of the ILIKE predicate.
     */
    default LikePredicate ilike(Expression pattern) {
        return LikePredicate.of(LikeMode.ILIKE, this, pattern, false);
    }

    /**
     * Creates ILIKE predicate for the current expression.
     *
     * @param pattern a pattern to use in ILIKE predicate.
     * @return A newly created instance of the ILIKE predicate.
     */
    default LikePredicate ilike(String pattern) {
        return LikePredicate.of(LikeMode.ILIKE, this, literal(pattern), false);
    }

    /**
     * Creates NOT ILIKE predicate for the current expression.
     *
     * @param pattern a pattern to use in NOT ILIKE predicate.
     * @return A newly created instance of the NOT ILIKE predicate.
     */
    default LikePredicate notIlike(Expression pattern) {
        return LikePredicate.of(LikeMode.ILIKE, this, pattern, true);
    }

    /**
     * Creates NOT ILIKE predicate for the current expression.
     *
     * @param pattern a pattern to use in NOT ILIKE predicate.
     * @return A newly created instance of the NOT ILIKE predicate.
     */
    default LikePredicate notIlike(String pattern) {
        return LikePredicate.of(LikeMode.ILIKE, this, literal(pattern), true);
    }

    /**
     * Creates SIMILAR TO predicate for the current expression.
     *
     * @param pattern a pattern to use in SIMILAR TO predicate.
     * @return A newly created instance of the SIMILAR TO predicate.
     */
    default LikePredicate similarTo(Expression pattern) {
        return LikePredicate.of(LikeMode.SIMILAR_TO, this, pattern, false);
    }

    /**
     * Creates SIMILAR TO predicate for the current expression.
     *
     * @param pattern a pattern to use in SIMILAR TO predicate.
     * @return A newly created instance of the SIMILAR TO predicate.
     */
    default LikePredicate similarTo(String pattern) {
        return LikePredicate.of(LikeMode.SIMILAR_TO, this, literal(pattern), false);
    }

    /**
     * Creates NOT SIMILAR TO predicate for the current expression.
     *
     * @param pattern a pattern to use in NOT SIMILAR TO predicate.
     * @return A newly created instance of the NOT SIMILAR TO predicate.
     */
    default LikePredicate notSimilarTo(Expression pattern) {
        return LikePredicate.of(LikeMode.SIMILAR_TO, this, pattern, true);
    }

    /**
     * Creates NOT SIMILAR TO predicate for the current expression.
     *
     * @param pattern a pattern to use in NOT SIMILAR TO predicate.
     * @return A newly created instance of the NOT SIMILAR TO predicate.
     */
    default LikePredicate notSimilarTo(String pattern) {
        return LikePredicate.of(LikeMode.SIMILAR_TO, this, literal(pattern), true);
    }

    /**
     * Creates IS NULL predicate for the current expression.
     *
     * @return A newly created instance of the IS NULL predicate.
     */
    default IsNullPredicate isNull() {
        return IsNullPredicate.of(this, false);
    }

    /**
     * Creates IS NOT NULL predicate for the current expression.
     *
     * @return A newly created instance of the IS NOT NULL predicate.
     */
    default IsNullPredicate isNotNull() {
        return IsNullPredicate.of(this, true);
    }

    /**
     * Creates ANY predicate for the current expression.
     * <p>For example:</p>
     * <pre>
     *     {@code
     *     age <= ANY (SELECT age FROM users WHERE active = true)
     *     }
     * </pre>
     *
     * @param operator a comparison operator to be used.
     * @param subquery a sub query providing set of values to compare with.
     * @return A newly created ANY predicate.
     */
    default AnyAllPredicate any(ComparisonOperator operator, Query subquery) {
        return AnyAllPredicate.of(this, operator, subquery, Quantifier.ANY);
    }

    /**
     * Creates ALL predicate for the current expression.
     * <p>For example:</p>
     * <pre>
     *     {@code
     *     salary > ALL (SELECT salary FROM employees WHERE department = 'HR')
     *     }
     * </pre>
     *
     * @param operator a comparison operator to be used.
     * @param subquery a sub query providing set of values to compare with.
     * @return A newly created ALL predicate.
     */
    default AnyAllPredicate all(ComparisonOperator operator, Query subquery) {
        return AnyAllPredicate.of(this, operator, subquery, Quantifier.ALL);
    }

    /**
     * Creates a unary predicate.
     * <p>For example:</p>
     * <pre>
     *     {@code
     *     WHERE true
     *     WHERE active
     *     }
     * </pre>
     *
     * @return unary operator based on current expression.
     */
    default UnaryPredicate unary() {
        return UnaryPredicate.of(this);
    }

    /**
     * Creates a binary operator expression using this expression as the left operand.
     * <pre>{@code
     * left.op("->", right)  // renders as: left -> right
     * }</pre>
     *
     * @param operator operator token
     * @param right    right operand
     * @return binary operator expression
     */
    default BinaryOperatorExpr op(String operator, Expression right) {
        return BinaryOperatorExpr.of(this, operator, right);
    }

    /**
     * Creates a unary operator expression applying {@code operator} to this expression.
     * <pre>{@code
     * expr.unary("-") // renders as: -expr
     * }</pre>
     *
     * @param operator operator token
     * @return unary operator expression
     */
    default UnaryOperatorExpr unary(String operator) {
        return UnaryOperatorExpr.of(operator, this);
    }

    /**
     * Creates a cast expression that casts this expression to the given type.
     * <p>
     * This models {@code CAST(<expr> AS <type>)} or dialect shorthand such as PostgreSQL {@code (<expr>)::type}.
     *
     * <pre>{@code
     * lit("{\"a\":1}").cast("jsonb")  // '{"a":1}'::jsonb
     * lit("{a,b,0}").cast("text[]")   // '{a,b,0}'::text[]
     * }</pre>
     *
     * @param type target type name (for example {@code "jsonb"}, {@code "text[]"}, {@code "bigint"})
     * @return cast expression
     */
    default CastExpr cast(TypeName type) {
        return CastExpr.of(this, type);
    }

    /**
     * Creates a new matcher for the current {@link Expression}.
     *
     * @param <R> the result type produced by the match
     * @return a new {@code ExpressionMatch} for current expression.
     */
    default <R> ExpressionMatch<R> matchExpression() {
        return ExpressionMatch.match(this);
    }
}
