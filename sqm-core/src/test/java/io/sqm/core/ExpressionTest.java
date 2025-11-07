package io.sqm.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExpressionTest {

    @Test
    void literal() {
        assertInstanceOf(LiteralExpr.class, Expression.literal("a"));
    }

    @Test
    void illegalLiteral() {
        assertThrowsExactly(IllegalArgumentException.class, () -> Expression.literal(Expression.literal("a")));
    }

    @Test
    void column() {
        assertInstanceOf(ColumnExpr.class, Expression.column("c"));
    }

    @Test
    void testColumn() {
        assertInstanceOf(ColumnExpr.class, Expression.column("t", "c"));
    }

    @Test
    void funcNoArgs() {
        var func = Expression.func("f");
        assertInstanceOf(FunctionExpr.class, func);
        assertTrue(func.args().isEmpty());
    }

    @Test
    void funcWithArgs() {
        var func = Expression.func("f", Expression.funcArg(Expression.literal(1)));
        assertInstanceOf(FunctionExpr.class, func);
        assertEquals(1, func.args().size());
    }

    @Test
    void funcArg() {
        assertInstanceOf(FunctionExpr.Arg.ExprArg.class, Expression.funcArg(Expression.literal(1)));
    }

    @Test
    void starArg() {
        assertInstanceOf(FunctionExpr.Arg.StarArg.class, Expression.starArg());
    }

    @Test
    void kase() {
        var whenThen = WhenThen.when(Expression.column("c1").eq(Expression.column("c2"))).then(Expression.literal(true));
        var kase = Expression.kase(whenThen);
        assertInstanceOf(CaseExpr.class, kase);
        assertEquals(1, kase.whens().size());
        kase = Expression.kase(List.of(whenThen));
        assertInstanceOf(CaseExpr.class, kase);
        assertEquals(1, kase.whens().size());
    }

    @Test
    void subquery() {
        assertInstanceOf(QueryExpr.class, Expression.subquery(Query.select(Expression.literal(1))));
    }

    @Test
    void row() {
        var row = Expression.row(1, 2, 3);
        assertInstanceOf(RowExpr.class, row);
        assertEquals(3, row.items().size());
    }

    @Test
    void rows() {
        var rows = Expression.rows(
            Expression.row(1, 2),
            Expression.row(3, 4)
        );
        assertInstanceOf(RowListExpr.class, rows);
        assertEquals(2, rows.rows().size());
        assertEquals(2, rows.rows().get(0).items().size());
        assertEquals(2, rows.rows().get(1).items().size());
    }

    @Test
    void asCase() {
        var whenThen = WhenThen.when(Expression.column("c1").eq(Expression.column("c2"))).then(Expression.literal(true));
        Expression expr = Expression.kase(whenThen);
        assertTrue(expr.asCase().isPresent());
        assertFalse(Expression.column("c").asCase().isPresent());
    }

    @Test
    void asColumn() {
        Expression expr = Expression.column("c");
        assertTrue(expr.asColumn().isPresent());
        assertFalse(Expression.literal("c").asColumn().isPresent());
    }

    @Test
    void asFunc() {
        Expression expr = Expression.func("f", Expression.funcArg(Expression.literal(1)));
        assertTrue(expr.asFunc().isPresent());
        assertFalse(Expression.literal("c").asFunc().isPresent());
    }

    @Test
    void asFuncArg() {
        Expression expr = Expression.funcArg(Expression.literal(1));
        assertTrue(expr.asFuncArg().isPresent());
        assertFalse(Expression.literal("c").asFuncArg().isPresent());
    }

    @Test
    void asExprArg() {
        Expression expr = Expression.funcArg(Expression.literal(1));
        assertTrue(expr.asExprArg().isPresent());
        assertFalse(Expression.literal("c").asExprArg().isPresent());
    }

    @Test
    void asStarArg() {
        Expression expr = Expression.starArg();
        assertTrue(expr.asStarArg().isPresent());
        assertFalse(Expression.literal("c").asStarArg().isPresent());
    }

    @Test
    void asLiteral() {
        Expression expr = Expression.literal(1);
        assertTrue(expr.asLiteral().isPresent());
        assertFalse(Expression.column("c").asLiteral().isPresent());
    }

    @Test
    void asPredicate() {
        Expression expr = Expression.literal(1).eq(Expression.literal(1));
        assertTrue(expr.asPredicate().isPresent());
        assertFalse(Expression.literal("c").asPredicate().isPresent());
    }

    @Test
    void asValues() {
        Expression expr = Expression.row(1, 2);
        assertTrue(expr.asValues().isPresent());
        assertFalse(Expression.literal("c").asValues().isPresent());
    }

    @Test
    void as() {
        assertInstanceOf(SelectItem.class, Expression.literal(1).as("const"));
    }

    @Test
    void toSelectItem() {
        assertInstanceOf(SelectItem.class, Expression.literal(1).toSelectItem());
    }

    @Test
    void eq() {
        assertInstanceOf(ComparisonPredicate.class, Expression.literal(1).eq(Expression.literal(1)));
        assertInstanceOf(ComparisonPredicate.class, Expression.literal(1).eq(1));
    }

    @Test
    void ne() {
        assertInstanceOf(ComparisonPredicate.class, Expression.literal(1).ne(Expression.literal(1)));
        assertInstanceOf(ComparisonPredicate.class, Expression.literal(1).ne(1));
    }

    @Test
    void lt() {
        assertInstanceOf(ComparisonPredicate.class, Expression.literal(1).lt(Expression.literal(2)));
        assertInstanceOf(ComparisonPredicate.class, Expression.literal(1).lt(2));
    }

    @Test
    void lte() {
        assertInstanceOf(ComparisonPredicate.class, Expression.literal(1).lte(Expression.literal(2)));
        assertInstanceOf(ComparisonPredicate.class, Expression.literal(1).lte(2));
    }

    @Test
    void gt() {
        assertInstanceOf(ComparisonPredicate.class, Expression.literal(1).gt(Expression.literal(2)));
        assertInstanceOf(ComparisonPredicate.class, Expression.literal(1).gt(2));
    }

    @Test
    void gte() {
        assertInstanceOf(ComparisonPredicate.class, Expression.literal(1).gte(Expression.literal(2)));
        assertInstanceOf(ComparisonPredicate.class, Expression.literal(1).gte(2));
    }

    @Test
    void in() {
        assertInstanceOf(InPredicate.class, Expression.column("c").in(1, 2, 3));
        assertInstanceOf(InPredicate.class, Expression.column("c").in(Expression.row(1, 2, 3)));
    }

    @Test
    void notIn() {
        assertInstanceOf(InPredicate.class, Expression.column("c").notIn(1, 2, 3));
        assertInstanceOf(InPredicate.class, Expression.column("c").notIn(Expression.row(1, 2, 3)));
    }

    @Test
    void between() {
        assertInstanceOf(BetweenPredicate.class, Expression.column("c").between(1, 10));
        assertInstanceOf(BetweenPredicate.class, Expression.column("c").between(Expression.literal(1), Expression.literal(10)));
    }

    @Test
    void like() {
        assertInstanceOf(LikePredicate.class, Expression.column("c").like("%abc"));
        assertInstanceOf(LikePredicate.class, Expression.column("c").like(Expression.literal("%abc")));
    }

    @Test
    void notLike() {
        assertInstanceOf(LikePredicate.class, Expression.column("c").notLike("%abc"));
        assertInstanceOf(LikePredicate.class, Expression.column("c").notLike(Expression.literal("%abc")));
    }

    @Test
    void isNull() {
        var isNull = Expression.column("c").isNull();
        assertInstanceOf(IsNullPredicate.class, isNull);
        assertFalse(isNull.negated());
    }

    @Test
    void isNotNull() {
        var isNull = Expression.column("c").isNotNull();
        assertInstanceOf(IsNullPredicate.class, isNull);
        assertTrue(isNull.negated());
    }

    @Test
    void any() {
        var any = Expression.column("c").any(ComparisonOperator.EQ, Query.select(Expression.literal(1)));
        assertInstanceOf(AnyAllPredicate.class, any);
        assertEquals(Quantifier.ANY, any.quantifier());
    }

    @Test
    void all() {
        var all = Expression.column("c").all(ComparisonOperator.EQ, Query.select(Expression.literal(1)));
        assertInstanceOf(AnyAllPredicate.class, all);
        assertEquals(Quantifier.ALL, all.quantifier());
    }
}