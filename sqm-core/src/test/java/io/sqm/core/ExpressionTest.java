package io.sqm.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.*;

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
        assertInstanceOf(ColumnExpr.class, col("c"));
    }

    @Test
    void testColumn() {
        assertInstanceOf(ColumnExpr.class, col("t", "c"));
    }

    @Test
    void funcNoArgs() {
        var func = func("f");
        assertInstanceOf(FunctionExpr.class, func);
        assertTrue(func.args().isEmpty());
    }

    @Test
    void funcWithArgs() {
        var func = func("f", Expression.funcArg(Expression.literal(1)));
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
        var whenThen = WhenThen.when(col("c1").eq(col("c2"))).then(Expression.literal(true));
        var kase = Expression.kase(whenThen);
        assertInstanceOf(CaseExpr.class, kase);
        assertEquals(1, kase.whens().size());
        kase = Expression.kase(List.of(whenThen));
        assertInstanceOf(CaseExpr.class, kase);
        assertEquals(1, kase.whens().size());
    }

    @Test
    void subquery() {
        assertInstanceOf(QueryExpr.class, Expression.subquery(Query.select(Expression.literal(1)).build()));
    }

    @Test
    void row() {
        var row = Expression.row(1, 2, 3);
        var literal = Expression.literal(1);
        var mixed = Expression.row(literal, 2);
        assertInstanceOf(RowExpr.class, row);
        assertEquals(3, row.items().size());
        assertEquals(2, mixed.items().size());
        assertSame(literal, mixed.items().getFirst());
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
    void as() {
        assertInstanceOf(SelectItem.class, Expression.literal(1).as("const"));
    }

    @Test
    void asIdentifier() {
        var item = assertInstanceOf(ExprSelectItem.class, Expression.literal(1).as(Identifier.of("const", QuoteStyle.DOUBLE_QUOTE)));
        assertEquals("const", item.alias().value());
        assertEquals(QuoteStyle.DOUBLE_QUOTE, item.alias().quoteStyle());
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
    void nullSafeEq() {
        assertInstanceOf(ComparisonPredicate.class, Expression.literal(1).nullSafeEq(Expression.literal(1)));
        assertInstanceOf(ComparisonPredicate.class, Expression.literal(1).nullSafeEq(1));
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
        assertInstanceOf(InPredicate.class, col("c").in(1, 2, 3));
        assertInstanceOf(InPredicate.class, col("c").in(Expression.row(1, 2, 3)));
    }

    @Test
    void notIn() {
        assertInstanceOf(InPredicate.class, col("c").notIn(1, 2, 3));
        assertInstanceOf(InPredicate.class, col("c").notIn(Expression.row(1, 2, 3)));
    }

    @Test
    void between() {
        assertInstanceOf(BetweenPredicate.class, col("c").between(1, 10));
        assertInstanceOf(BetweenPredicate.class, col("c").between(Expression.literal(1), Expression.literal(10)));
    }

    @Test
    void like() {
        assertInstanceOf(LikePredicate.class, col("c").like("%abc"));
        assertInstanceOf(LikePredicate.class, col("c").like(Expression.literal("%abc")));
    }

    @Test
    void notLike() {
        assertInstanceOf(LikePredicate.class, col("c").notLike("%abc"));
        assertInstanceOf(LikePredicate.class, col("c").notLike(Expression.literal("%abc")));
    }

    @Test
    void likeModeConvenienceMethods() {
        var ilikeExpr = col("c").ilike(lit("%abc"));
        var ilikeString = col("c").ilike("%abc");
        var notIlikeExpr = col("c").notIlike(lit("%abc"));
        var notIlikeString = col("c").notIlike("%abc");
        var similarExpr = col("c").similarTo(lit("%abc"));
        var similarString = col("c").similarTo("%abc");
        var notSimilarExpr = col("c").notSimilarTo(lit("%abc"));
        var notSimilarString = col("c").notSimilarTo("%abc");

        assertEquals(LikeMode.ILIKE, ilikeExpr.mode());
        assertFalse(ilikeExpr.negated());
        assertEquals(LikeMode.ILIKE, ilikeString.mode());
        assertTrue(notIlikeExpr.negated());
        assertTrue(notIlikeString.negated());
        assertEquals(LikeMode.SIMILAR_TO, similarExpr.mode());
        assertFalse(similarExpr.negated());
        assertEquals(LikeMode.SIMILAR_TO, similarString.mode());
        assertTrue(notSimilarExpr.negated());
        assertTrue(notSimilarString.negated());
    }

    @Test
    void isNull() {
        var isNull = col("c").isNull();
        assertInstanceOf(IsNullPredicate.class, isNull);
        assertFalse(isNull.negated());
    }

    @Test
    void isNotNull() {
        var isNull = col("c").isNotNull();
        assertInstanceOf(IsNullPredicate.class, isNull);
        assertTrue(isNull.negated());
    }

    @Test
    void any() {
        var any = col("c").any(ComparisonOperator.EQ, Query.select(Expression.literal(1)).build());
        assertInstanceOf(AnyAllPredicate.class, any);
        assertEquals(Quantifier.ANY, any.quantifier());
    }

    @Test
    void all() {
        var all = col("c").all(ComparisonOperator.EQ, Query.select(Expression.literal(1)).build());
        assertInstanceOf(AnyAllPredicate.class, all);
        assertEquals(Quantifier.ALL, all.quantifier());
    }

    @Test
    void unary() {
        Predicate unary = Expression.literal(true).unary();
        assertInstanceOf(UnaryPredicate.class, unary);
        assertTrue(unary.<Boolean>matchPredicate().unary(u -> true).orElse(false));
    }

    @Test
    void powExpression() {
        var expr = Expression.literal(2).pow(Expression.literal(3));
        assertInstanceOf(PowerArithmeticExpr.class, expr);
        assertEquals(2, expr.lhs().matchExpression().literal(l -> l.value()).orElse(null));
        assertEquals(3, expr.rhs().matchExpression().literal(l -> l.value()).orElse(null));
    }

    @Test
    void powIntExpression() {
        var expr = Expression.literal(4).pow(2);
        assertInstanceOf(PowerArithmeticExpr.class, expr);
        assertEquals(4, expr.lhs().matchExpression().literal(l -> l.value()).orElse(null));
        assertEquals(2, expr.rhs().matchExpression().literal(l -> l.value()).orElse(null));
    }

    @Test
    void concatExpression() {
        var expr = col("first_name").concat(lit(" "));
        assertInstanceOf(ConcatExpr.class, expr);
        assertEquals(2, expr.args().size());
    }

    @Test
    void arithmeticArrayOperatorAndResultConveniences() {
        var resultItem = col("name").toResultItem();
        var mod = lit(5).mod(lit(2));
        var subscript = col("items").at(lit(1));
        var subscriptInt = col("items").at(2);
        var slice = col("items").slice(lit(1), lit(3));
        var sliceInts = col("items").slice(1, 3);
        var sliceFrom = col("items").sliceFrom(2);
        var sliceTo = col("items").sliceTo(4);
        var regex = col("name").regex(RegexMode.MATCH_INSENSITIVE, lit("^a"), true);
        var op = col("payload").op("->>", lit("name"));
        var typedOp = col("payload").op(OperatorName.operator(QualifiedName.of("pg_catalog"), "?"), lit("name"));
        var unaryOp = col("flags").unary("~");
        var cast = lit("1").cast(type("int"));
        var collate = col("name").collate(QualifiedName.of("pg_catalog", "en_US"));
        var atTimeZone = col("created_at").atTimeZone(lit("UTC"));

        assertInstanceOf(ResultItem.class, resultItem);
        assertEquals(5, mod.lhs().matchExpression().literal(l -> l.value()).orElse(null));
        assertEquals(1, subscript.index().matchExpression().literal(l -> l.value()).orElse(null));
        assertEquals(2, subscriptInt.index().matchExpression().literal(l -> l.value()).orElse(null));
        assertTrue(slice.from().isPresent());
        assertTrue(slice.to().isPresent());
        assertEquals(1, sliceInts.from().orElseThrow().matchExpression().literal(l -> l.value()).orElse(null));
        assertTrue(sliceFrom.from().isPresent());
        assertTrue(sliceFrom.to().isEmpty());
        assertTrue(sliceTo.from().isEmpty());
        assertTrue(sliceTo.to().isPresent());
        assertEquals(RegexMode.MATCH_INSENSITIVE, regex.mode());
        assertTrue(regex.negated());
        assertEquals("->>", op.operator().symbol());
        assertEquals(List.of("pg_catalog"), typedOp.operator().schemaName().values());
        assertEquals("~", unaryOp.operator().symbol());
        assertEquals(List.of("int"), cast.type().qualifiedName().values());
        assertEquals(List.of("pg_catalog", "en_US"), collate.collation().values());
        assertEquals("UTC", atTimeZone.timezone().matchExpression().literal(l -> l.value()).orElse(null));
    }
}
