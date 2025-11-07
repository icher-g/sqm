package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PredicateTest {

    @Test
    void not() {
        assertInstanceOf(NotPredicate.class, Predicate.not(Expression.column("c").eq(1)));
    }

    @Test
    void unary() {
        assertInstanceOf(UnaryPredicate.class, Predicate.unary(Expression.literal(true)));
    }

    @Test
    void exists() {
        var exists = Predicate.exists(Query.select(Expression.literal(1)));
        assertInstanceOf(ExistsPredicate.class, exists);
        assertFalse(exists.negated());
    }

    @Test
    void notExists() {
        var exists = Predicate.notExists(Query.select(Expression.literal(1)));
        assertInstanceOf(ExistsPredicate.class, exists);
        assertTrue(exists.negated());
    }

    @Test
    void and() {
        var p1 = Expression.column("c1").eq(1);
        var p2 = Expression.column("c2").eq(1);
        var and = p1.and(p2);
        assertInstanceOf(AndPredicate.class, and);
        assertInstanceOf(ComparisonPredicate.class, and.lhs());
        assertInstanceOf(ComparisonPredicate.class, and.rhs());
    }

    @Test
    void or() {
        var p1 = Expression.column("c1").eq(1);
        var p2 = Expression.column("c2").eq(1);
        var or = p1.or(p2);
        assertInstanceOf(OrPredicate.class, or);
        assertInstanceOf(ComparisonPredicate.class, or.lhs());
        assertInstanceOf(ComparisonPredicate.class, or.rhs());
    }

    @Test
    void expressionInvertedAsNot() {
        assertInstanceOf(NotPredicate.class, Expression.column("c").eq(1).not());
    }

    @Test
    void asAnyAll() {
        Predicate any = Expression.column("c").any(ComparisonOperator.EQ, Query.select(Expression.literal(1)));
        Predicate all = Expression.column("c").all(ComparisonOperator.EQ, Query.select(Expression.literal(1)));
        assertTrue(any.asAnyAll().isPresent());
        assertTrue(all.asAnyAll().isPresent());
        assertFalse(Expression.column("c").eq(1).asAnyAll().isPresent());
    }

    @Test
    void asBetween() {
        Predicate p = Expression.column("c").between(1, 10);
        assertTrue(p.asBetween().isPresent());
        assertFalse(Expression.column("c").eq(1).asBetween().isPresent());
    }

    @Test
    void asComparison() {
        Predicate p = Expression.column("c").eq(1);
        assertTrue(p.asComparison().isPresent());
        assertFalse(Expression.column("c").between(1, 10).asComparison().isPresent());
    }

    @Test
    void asComposite() {
        Predicate p1 = Expression.column("c1").eq(1);
        Predicate p2 = Expression.column("c2").eq(1);
        Predicate and = p1.and(p2);
        assertTrue(and.asComposite().isPresent());
        assertFalse(p1.asComposite().isPresent());
    }

    @Test
    void asExists() {
        Predicate exists = Predicate.notExists(Query.select(Expression.literal(1)));
        assertTrue(exists.asExists().isPresent());
        assertFalse(Expression.column("c1").eq(1).asExists().isPresent());
    }

    @Test
    void asIn() {
        Predicate p = Expression.column("c").in(1, 2, 3);
        assertTrue(p.asIn().isPresent());
        assertFalse(Expression.column("c1").eq(1).asIn().isPresent());
    }

    @Test
    void asIsNull() {
        Predicate p = Expression.column("c").isNull();
        assertTrue(p.asIsNull().isPresent());
        assertFalse(Expression.column("c1").eq(1).asIsNull().isPresent());
    }

    @Test
    void asLike() {
        Predicate p = Expression.column("c").like("%abc");
        assertTrue(p.asLike().isPresent());
        assertFalse(Expression.column("c1").eq(1).asLike().isPresent());
    }

    @Test
    void asNot() {
        Predicate p = Expression.column("c").eq(1).not();
        assertTrue(p.asNot().isPresent());
        assertFalse(Expression.column("c1").eq(1).asNot().isPresent());
    }
}