package io.sqm.core;

import io.sqm.core.internal.FuncCallArg;
import io.sqm.core.internal.FuncColumnArg;
import io.sqm.core.internal.FuncLiteralArg;
import io.sqm.core.internal.FuncStarArg;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Any value-producing node (scalar or boolean).
 */
public sealed interface Expression extends Node
    permits CaseExpr, ColumnExpr, FunctionExpr, FunctionExpr.Arg, LiteralExpr, Predicate, ValueSet {

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
     * Creates a function call expression.
     *
     * @param name        a function name
     * @param distinctArg indicates whether DISTINCT should be added before the list of arguments in the function call. {@code COUNT(DISTINCT t.id) AS c}
     * @param args        an array of function arguments.
     * @return A newly created instance of a function call expression.
     */
    static FunctionExpr func(String name, boolean distinctArg, FunctionExpr.Arg... args) {
        return FunctionExpr.of(name, distinctArg, args);
    }

    /**
     * Creates a function argument that wraps a column reference.
     *
     * @param column a column reference to wrap.
     * @return A newly created instance of a function argument.
     */
    static FunctionExpr.Arg.Column funcArg(ColumnExpr column) {
        return new FuncColumnArg(column);
    }

    /**
     * Creates a function argument that wraps a literal value.
     *
     * @param value a literal value.
     * @return A newly created instance of a function argument.
     */
    static FunctionExpr.Arg.Literal funcArg(Object value) {
        return new FuncLiteralArg(value);
    }

    /**
     * Creates a function argument that wraps another function call.
     *
     * @param call a function call.
     * @return A newly created instance of a function argument.
     */
    static FunctionExpr.Arg.Function funcArg(FunctionExpr call) {
        return new FuncCallArg(call);
    }

    /**
     * Creates a function argument that wraps a '*'.
     *
     * @return A newly created instance of a function argument.
     */
    static FunctionExpr.Arg.Star starArg() {
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
     * Casts current expression to {@link CaseExpr} if possible.
     *
     * @return an {@link Optional}<{@link CaseExpr}>.
     */
    default Optional<CaseExpr> asCase() {
        return this instanceof CaseExpr e ? Optional.of(e) : Optional.empty();
    }

    /**
     * Casts current expression to {@link ColumnExpr} if possible.
     *
     * @return an {@link Optional}<{@link ColumnExpr}>.
     */
    default Optional<ColumnExpr> asColumn() {
        return this instanceof ColumnExpr e ? Optional.of(e) : Optional.empty();
    }

    /**
     * Casts current expression to {@link FunctionExpr} if possible.
     *
     * @return an {@link Optional}<{@link FunctionExpr}>.
     */
    default Optional<FunctionExpr> asFunc() {
        return this instanceof FunctionExpr e ? Optional.of(e) : Optional.empty();
    }

    /**
     * Casts current expression to {@link FunctionExpr.Arg} if possible.
     *
     * @return an {@link Optional}<{@link FunctionExpr.Arg}>.
     */
    default Optional<FunctionExpr.Arg> asFuncArg() {
        return this instanceof FunctionExpr.Arg a ? Optional.of(a) : Optional.empty();
    }

    /**
     * Casts current expression to {@link LiteralExpr} if possible.
     *
     * @return an {@link Optional}<{@link LiteralExpr}>.
     */
    default Optional<LiteralExpr> asLiteral() {
        return this instanceof LiteralExpr e ? Optional.of(e) : Optional.empty();
    }

    /**
     * Casts current expression to {@link Predicate} if possible.
     *
     * @return an {@link Optional}<{@link Predicate}>.
     */
    default Optional<Predicate> asPredicate() {
        return this instanceof Predicate p ? Optional.of(p) : Optional.empty();
    }

    /**
     * Casts current expression to {@link ValueSet} if possible.
     *
     * @return an {@link Optional}<{@link ValueSet}>.
     */
    default Optional<ValueSet> asValues() {
        return this instanceof ValueSet v ? Optional.of(v) : Optional.empty();
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
        return BetweenPredicate.of(this, lower, upper, false);
    }

    /**
     * Creates BETWEEN predicate for the current expression.
     *
     * @param lower a lower part of the predicate.
     * @param upper an upper part of the predicate.
     * @return A newly created instance of the BETWEEN predicate.
     */
    default BetweenPredicate between(Object lower, Object upper) {
        return BetweenPredicate.of(this, literal(lower), literal(upper), false);
    }

    /**
     * Creates LIKE predicate for the current expression.
     *
     * @param pattern a pattern to use in LIKE predicate.
     * @return A newly created instance of the LIKE predicate.
     */
    default LikePredicate like(Expression pattern) {
        return LikePredicate.of(this, pattern, null, true);
    }

    /**
     * Creates LIKE predicate for the current expression.
     *
     * @param pattern a pattern to use in LIKE predicate.
     * @return A newly created instance of the LIKE predicate.
     */
    default LikePredicate like(String pattern) {
        return LikePredicate.of(this, literal(pattern), null, false);
    }

    /**
     * Creates NOT LIKE predicate for the current expression.
     *
     * @param pattern a pattern to use in NOT LIKE predicate.
     * @return A newly created instance of the NOT LIKE predicate.
     */
    default LikePredicate notLike(Expression pattern) {
        return LikePredicate.of(this, pattern, null, true);
    }

    /**
     * Creates NOT LIKE predicate for the current expression.
     *
     * @param pattern a pattern to use in NOT LIKE predicate.
     * @return A newly created instance of the NOT LIKE predicate.
     */
    default LikePredicate notLike(String pattern) {
        return LikePredicate.of(this, literal(pattern), null, true);
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
}
