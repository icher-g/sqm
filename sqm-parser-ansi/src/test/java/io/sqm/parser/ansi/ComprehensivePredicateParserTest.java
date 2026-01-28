package io.sqm.parser.ansi;

import io.sqm.core.*;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for predicate parsing covering various predicate types.
 * This increases coverage for AtomicPredicateParser and related base parser classes.
 */
class ComprehensivePredicateParserTest {

    private ParseContext ctx;

    @BeforeEach
    void setUp() {
        ctx = ParseContext.of(new AnsiSpecs());
    }

    @Test
    void parsesComparisonEqual() {
        var result = ctx.parse(Predicate.class, "age = 25");
        assertTrue(result.ok());
        var pred = result.value();
        assertInstanceOf(ComparisonPredicate.class, pred);
        assertEquals(ComparisonOperator.EQ, ((ComparisonPredicate) pred).operator());
    }

    @Test
    void parsesComparisonNotEqual() {
        var result = ctx.parse(Predicate.class, "status <> 'active'");
        assertTrue(result.ok());
        var pred = result.value();
        assertInstanceOf(ComparisonPredicate.class, pred);
        assertEquals(ComparisonOperator.NE, ((ComparisonPredicate) pred).operator());
    }

    @Test
    void parsesComparisonLessThan() {
        var result = ctx.parse(Predicate.class, "price < 100");
        assertTrue(result.ok());
        var pred = result.value();
        assertInstanceOf(ComparisonPredicate.class, pred);
        assertEquals(ComparisonOperator.LT, ((ComparisonPredicate) pred).operator());
    }

    @Test
    void parsesComparisonLessThanOrEqual() {
        var result = ctx.parse(Predicate.class, "quantity <= 50");
        assertTrue(result.ok());
        var pred = result.value();
        assertInstanceOf(ComparisonPredicate.class, pred);
        assertEquals(ComparisonOperator.LTE, ((ComparisonPredicate) pred).operator());
    }

    @Test
    void parsesComparisonGreaterThan() {
        var result = ctx.parse(Predicate.class, "score > 80");
        assertTrue(result.ok());
        var pred = result.value();
        assertInstanceOf(ComparisonPredicate.class, pred);
        assertEquals(ComparisonOperator.GT, ((ComparisonPredicate) pred).operator());
    }

    @Test
    void parsesComparisonGreaterThanOrEqual() {
        var result = ctx.parse(Predicate.class, "age >= 18");
        assertTrue(result.ok());
        var pred = result.value();
        assertInstanceOf(ComparisonPredicate.class, pred);
        assertEquals(ComparisonOperator.GTE, ((ComparisonPredicate) pred).operator());
    }

    @Test
    void parsesBetweenPredicate() {
        var result = ctx.parse(Predicate.class, "age BETWEEN 18 AND 65");
        assertTrue(result.ok());
        var pred = result.value();
        assertInstanceOf(BetweenPredicate.class, pred);
        BetweenPredicate between = (BetweenPredicate) pred;
        assertFalse(between.negated());
    }

    @Test
    void parsesNotBetweenPredicate() {
        var result = ctx.parse(Predicate.class, "age NOT BETWEEN 18 AND 65");
        assertTrue(result.ok());
        var pred = result.value();
        assertInstanceOf(BetweenPredicate.class, pred);
        BetweenPredicate between = (BetweenPredicate) pred;
        assertTrue(between.negated());
    }

    @Test
    void parsesInPredicate() {
        var result = ctx.parse(Predicate.class, "status IN ('active', 'pending', 'approved')");
        assertTrue(result.ok());
        var pred = result.value();
        assertInstanceOf(InPredicate.class, pred);
        InPredicate in = (InPredicate) pred;
        assertFalse(in.negated());
    }

    @Test
    void parsesNotInPredicate() {
        var result = ctx.parse(Predicate.class, "status NOT IN ('deleted', 'archived')");
        assertTrue(result.ok());
        var pred = result.value();
        assertInstanceOf(InPredicate.class, pred);
        InPredicate in = (InPredicate) pred;
        assertTrue(in.negated());
    }

    @Test
    void parsesInWithSubquery() {
        var result = ctx.parse(Predicate.class, "user_id IN (SELECT id FROM users)");
        assertTrue(result.ok());
        var pred = result.value();
        assertInstanceOf(InPredicate.class, pred);
        InPredicate in = (InPredicate) pred;
        assertInstanceOf(QueryExpr.class, in.rhs());
    }

    @Test
    void parsesIsNullPredicate() {
        var result = ctx.parse(Predicate.class, "email IS NULL");
        assertTrue(result.ok());
        var pred = result.value();
        assertInstanceOf(IsNullPredicate.class, pred);
        IsNullPredicate isNull = (IsNullPredicate) pred;
        assertFalse(isNull.negated());
    }

    @Test
    void parsesIsNotNullPredicate() {
        var result = ctx.parse(Predicate.class, "email IS NOT NULL");
        assertTrue(result.ok());
        var pred = result.value();
        assertInstanceOf(IsNullPredicate.class, pred);
        IsNullPredicate isNull = (IsNullPredicate) pred;
        assertTrue(isNull.negated());
    }

    @Test
    void parsesLikePredicate() {
        var result = ctx.parse(Predicate.class, "name LIKE '%John%'");
        assertTrue(result.ok());
        var pred = result.value();
        assertInstanceOf(LikePredicate.class, pred);
        LikePredicate like = (LikePredicate) pred;
        assertFalse(like.negated());
    }

    @Test
    void parsesNotLikePredicate() {
        var result = ctx.parse(Predicate.class, "name NOT LIKE '%test%'");
        assertTrue(result.ok());
        var pred = result.value();
        assertInstanceOf(LikePredicate.class, pred);
        LikePredicate like = (LikePredicate) pred;
        assertTrue(like.negated());
    }

    @Test
    void parsesLikeWithEscape() {
        var result = ctx.parse(Predicate.class, "name LIKE '%\\%%' ESCAPE '\\'");
        assertTrue(result.ok());
        var pred = result.value();
        assertInstanceOf(LikePredicate.class, pred);
        LikePredicate like = (LikePredicate) pred;
        assertNotNull(like.escape());
    }

    @Test
    void parsesExistsPredicate() {
        var result = ctx.parse(Predicate.class, "EXISTS (SELECT 1 FROM users WHERE active = TRUE)");
        assertTrue(result.ok());
        var pred = result.value();
        assertInstanceOf(ExistsPredicate.class, pred);
        ExistsPredicate exists = (ExistsPredicate) pred;
        assertFalse(exists.negated());
    }

    @Test
    void parsesNotExistsPredicate() {
        var result = ctx.parse(Predicate.class, "NOT EXISTS (SELECT 1 FROM orders WHERE status = 'pending')");
        assertTrue(result.ok());
        var pred = result.value();
        assertInstanceOf(ExistsPredicate.class, pred);
        ExistsPredicate exists = (ExistsPredicate) pred;
        assertTrue(exists.negated());
    }

    @Test
    void parsesAndPredicate() {
        var result = ctx.parse(Predicate.class, "age > 18 AND active = TRUE");
        assertTrue(result.ok());
        var pred = result.value();
        assertInstanceOf(AndPredicate.class, pred);
    }

    @Test
    void parsesOrPredicate() {
        var result = ctx.parse(Predicate.class, "status = 'active' OR status = 'pending'");
        assertTrue(result.ok());
        var pred = result.value();
        assertInstanceOf(OrPredicate.class, pred);
    }

    @Test
    void parsesNotPredicate() {
        var result = ctx.parse(Predicate.class, "NOT active");
        assertTrue(result.ok());
        var pred = result.value();
        assertInstanceOf(NotPredicate.class, pred);
    }

    @Test
    void parsesComplexAndOrCombination() {
        var result = ctx.parse(Predicate.class, "age > 18 AND (status = 'active' OR status = 'pending')");
        assertTrue(result.ok());
        var pred = result.value();
        assertInstanceOf(AndPredicate.class, pred);
    }

     @Test
     void parsesPrecedenceWithNot() {
         var result = ctx.parse(Predicate.class, "NOT age > 18 AND active = TRUE");
         assertTrue(result.ok());
         var pred = result.value();
         assertInstanceOf(AndPredicate.class, pred);
     }

    @Test
    void parsesUnaryPredicateFromBooleanColumn() {
        var result = ctx.parse(Predicate.class, "active");
        assertTrue(result.ok());
        var pred = result.value();
        assertInstanceOf(UnaryPredicate.class, pred);
    }

    @Test
    void parsesAnyWithSubquery() {
        var result = ctx.parse(Predicate.class, "price > ANY (SELECT price FROM products WHERE category = 'books')");
        assertTrue(result.ok());
        var pred = result.value();
        assertInstanceOf(AnyAllPredicate.class, pred);
        AnyAllPredicate anyAll = (AnyAllPredicate) pred;
        assertEquals(Quantifier.ANY, anyAll.quantifier());
    }

    @Test
    void parsesAllWithSubquery() {
        var result = ctx.parse(Predicate.class, "price < ALL (SELECT price FROM products WHERE category = 'premium')");
        assertTrue(result.ok());
        var pred = result.value();
        assertInstanceOf(AnyAllPredicate.class, pred);
        AnyAllPredicate anyAll = (AnyAllPredicate) pred;
        assertEquals(Quantifier.ALL, anyAll.quantifier());
    }

    @Test
    void parsesIsDistinctFromPredicate() {
        var result = ctx.parse(Predicate.class, "value IS DISTINCT FROM reference");
        assertTrue(result.ok());
        var pred = result.value();
        assertInstanceOf(IsDistinctFromPredicate.class, pred);
        IsDistinctFromPredicate distinct = (IsDistinctFromPredicate) pred;
        assertFalse(distinct.negated());
    }

    @Test
    void parsesIsNotDistinctFromPredicate() {
        var result = ctx.parse(Predicate.class, "value IS NOT DISTINCT FROM reference");
        assertTrue(result.ok());
        var pred = result.value();
        assertInstanceOf(IsDistinctFromPredicate.class, pred);
        IsDistinctFromPredicate distinct = (IsDistinctFromPredicate) pred;
        assertTrue(distinct.negated());
    }

    @Test
    void parsesComparisonWithArithmetic() {
        var result = ctx.parse(Predicate.class, "price * quantity > 1000");
        assertTrue(result.ok());
        var pred = result.value();
        assertInstanceOf(ComparisonPredicate.class, pred);
    }

    @Test
    void parsesComparisonWithFunctions() {
        var result = ctx.parse(Predicate.class, "UPPER(name) = 'JOHN'");
        assertTrue(result.ok());
        var pred = result.value();
        assertInstanceOf(ComparisonPredicate.class, pred);
    }

    @Test
    void parsesTupleInPredicate() {
        var result = ctx.parse(Predicate.class, "(first_name, last_name) IN (('John', 'Doe'), ('Jane', 'Smith'))");
        assertTrue(result.ok());
        var pred = result.value();
        assertInstanceOf(InPredicate.class, pred);
    }
}
