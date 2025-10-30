package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PredicateContractsTest {

    @Test
    void any_all_predicate() {
        var p = AnyAllPredicate.of(
            ColumnExpr.of("c1"),
            ComparisonOperator.GT,
            Query.select(Expression.literal(1)),
            Quantifier.ALL);

        assertEquals("c1", p.lhs()
            .asColumn().map(ColumnExpr::name).orElseThrow());
        assertEquals(ComparisonOperator.GT, p.operator());
        assertEquals(1, p.subquery()
            .asSelect().map(SelectQuery::select).orElseThrow().get(0)
            .asExpr().map(ExprSelectItem::expr).orElseThrow()
            .asLiteral().map(LiteralExpr::value).orElseThrow());
    }

    @Test
    void between_predicate() {
        var p = BetweenPredicate.of(
            Expression.column("c"),
            Expression.literal(1),
            Expression.literal(10),
            false);

        assertEquals("c", p.value()
            .asColumn().map(ColumnExpr::name).orElseThrow());
        assertEquals(1, p.lower()
            .asLiteral().map(LiteralExpr::value).orElseThrow());
        assertEquals(10, p.upper()
            .asLiteral().map(LiteralExpr::value).orElseThrow());
        assertFalse(p.symmetric());
    }

    @Test
    void comparison_predicate() {
        var p = ComparisonPredicate.of(Expression.column("c"), ComparisonOperator.EQ, Expression.literal(1));

        assertEquals("c", p.lhs()
            .asColumn().map(ColumnExpr::name).orElseThrow());
        assertEquals(ComparisonOperator.EQ, p.operator());
        assertEquals(1, p.rhs()
            .asLiteral().map(LiteralExpr::value).orElseThrow());
    }

    @Test
    void and_predicate() {
        var p = AndPredicate.of(
            ComparisonPredicate.of(Expression.column("c1"), ComparisonOperator.EQ, Expression.literal(1)),
            ComparisonPredicate.of(Expression.column("c2"), ComparisonOperator.GT, Expression.literal(2))
        );

        assertInstanceOf(ComparisonPredicate.class, p.lhs());
        assertInstanceOf(ComparisonPredicate.class, p.rhs());
        assertEquals("c1", p.lhs()
            .asComparison().map(ComparisonPredicate::lhs).orElseThrow()
            .asColumn().map(ColumnExpr::name).orElseThrow());
        assertEquals(ComparisonOperator.EQ, p.lhs()
            .asComparison().map(ComparisonPredicate::operator).orElseThrow());
        assertEquals(1, p.lhs()
            .asComparison().map(ComparisonPredicate::rhs).orElseThrow()
            .asLiteral().map(LiteralExpr::value).orElseThrow());
        assertEquals("c2", p.rhs()
            .asComparison().map(ComparisonPredicate::lhs).orElseThrow()
            .asColumn().map(ColumnExpr::name).orElseThrow());
        assertEquals(ComparisonOperator.GT, p.rhs()
            .asComparison().map(ComparisonPredicate::operator).orElseThrow());
        assertEquals(2, p.rhs()
            .asComparison().map(ComparisonPredicate::rhs).orElseThrow()
            .asLiteral().map(LiteralExpr::value).orElseThrow());
    }

    @Test
    void or_predicate() {
        var p = OrPredicate.of(
            ComparisonPredicate.of(Expression.column("c1"), ComparisonOperator.EQ, Expression.literal(1)),
            ComparisonPredicate.of(Expression.column("c2"), ComparisonOperator.GT, Expression.literal(2))
        );

        assertInstanceOf(ComparisonPredicate.class, p.lhs());
        assertInstanceOf(ComparisonPredicate.class, p.rhs());
        assertEquals("c1", p.lhs()
            .asComparison().map(ComparisonPredicate::lhs).orElseThrow()
            .asColumn().map(ColumnExpr::name).orElseThrow());
        assertEquals(ComparisonOperator.EQ, p.lhs()
            .asComparison().map(ComparisonPredicate::operator).orElseThrow());
        assertEquals(1, p.lhs()
            .asComparison().map(ComparisonPredicate::rhs).orElseThrow()
            .asLiteral().map(LiteralExpr::value).orElseThrow());
        assertEquals("c2", p.rhs()
            .asComparison().map(ComparisonPredicate::lhs).orElseThrow()
            .asColumn().map(ColumnExpr::name).orElseThrow());
        assertEquals(ComparisonOperator.GT, p.rhs()
            .asComparison().map(ComparisonPredicate::operator).orElseThrow());
        assertEquals(2, p.rhs()
            .asComparison().map(ComparisonPredicate::rhs).orElseThrow()
            .asLiteral().map(LiteralExpr::value).orElseThrow());
    }

    @Test
    void exists_predicate() {
        var p = ExistsPredicate.of(Query.select(Expression.literal(1)), false);

        assertEquals(1, p.subquery()
            .asSelect().map(SelectQuery::select).orElseThrow().get(0)
            .asExpr().map(ExprSelectItem::expr).orElseThrow()
            .asLiteral().map(LiteralExpr::value).orElseThrow());
        assertFalse(p.negated());
    }

    @Test
    void in_predicate() {
        var p = InPredicate.of(Expression.column("c"), Expression.row(1), true);

        assertEquals("c", p.lhs().asColumn().map(ColumnExpr::name).orElseThrow());
        assertEquals(1, p.rhs()
            .asRow().map(RowExpr::items).orElseThrow().size());
        assertEquals(1, p.rhs()
            .asRow().map(RowExpr::items).orElseThrow().get(0)
            .asLiteral().map(LiteralExpr::value).orElseThrow());
        assertTrue(p.negated());
    }

    @Test
    void is_null_predicate() {
        var p = IsNullPredicate.of(Expression.column("c"), true);

        assertEquals("c", p.expr().asColumn().map(ColumnExpr::name).orElseThrow());
        assertTrue(p.negated());
    }

    @Test
    void like_predicate() {
        var p = LikePredicate.of(Expression.column("c"), Expression.literal("%c%"), Expression.literal("\\"), true);

        assertEquals("c", p.value().asColumn().map(ColumnExpr::name).orElseThrow());
        assertEquals("%c%", p.pattern().asLiteral().map(LiteralExpr::value).orElseThrow());
        assertEquals("\\", p.escape().asLiteral().map(LiteralExpr::value).orElseThrow());
        assertTrue(p.negated());
    }

    @Test
    void not_predicate() {
        var p = NotPredicate.of(ComparisonPredicate.of(Expression.column("c1"), ComparisonOperator.EQ, Expression.literal(1)));

        assertInstanceOf(ComparisonPredicate.class, p.inner());
        assertEquals("c1", p.inner()
            .asComparison().map(ComparisonPredicate::lhs).orElseThrow()
            .asColumn().map(ColumnExpr::name).orElseThrow());
        assertEquals(ComparisonOperator.EQ, p.inner()
            .asComparison().map(ComparisonPredicate::operator).orElseThrow());
        assertEquals(1, p.inner()
            .asComparison().map(ComparisonPredicate::rhs).orElseThrow()
            .asLiteral().map(LiteralExpr::value).orElseThrow());
    }

    @Test
    void unary_predicate() {
        var p = UnaryPredicate.of(Expression.literal(true));

        assertTrue((boolean)p.expr().asLiteral().map(LiteralExpr::value).orElseThrow());
    }
}
