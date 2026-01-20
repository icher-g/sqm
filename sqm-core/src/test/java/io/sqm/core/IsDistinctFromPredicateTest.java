package io.sqm.core;

import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.lit;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link IsDistinctFromPredicate}.
 */
class IsDistinctFromPredicateTest {

    @Test
    void testCreateIsDistinctFrom() {
        var lhs = col("a");
        var rhs = lit(10);
        var predicate = IsDistinctFromPredicate.of(lhs, rhs, false);

        assertNotNull(predicate);
        assertSame(lhs, predicate.lhs());
        assertSame(rhs, predicate.rhs());
        assertFalse(predicate.negated());
    }

    @Test
    void testCreateIsNotDistinctFrom() {
        var lhs = col("b");
        var rhs = col("c");
        var predicate = IsDistinctFromPredicate.of(lhs, rhs, true);

        assertNotNull(predicate);
        assertSame(lhs, predicate.lhs());
        assertSame(rhs, predicate.rhs());
        assertTrue(predicate.negated());
    }

    @Test
    void testEquality() {
        var lhs = col("x");
        var rhs = lit(5);
        var pred1 = IsDistinctFromPredicate.of(lhs, rhs, false);
        var pred2 = IsDistinctFromPredicate.of(lhs, rhs, false);
        var pred3 = IsDistinctFromPredicate.of(lhs, rhs, true);

        assertEquals(pred1, pred2);
        assertNotEquals(pred1, pred3);
        assertEquals(pred1.hashCode(), pred2.hashCode());
    }

    @Test
    void testWithDifferentExpressions() {
        var col1 = col("a");
        var col2 = col("b");
        var pred1 = IsDistinctFromPredicate.of(col1, lit(1), false);
        var pred2 = IsDistinctFromPredicate.of(col2, lit(1), false);
        var pred3 = IsDistinctFromPredicate.of(col1, lit(2), false);

        assertNotEquals(pred1, pred2, "Different LHS should not be equal");
        assertNotEquals(pred1, pred3, "Different RHS should not be equal");
    }

    @Test
    void testAcceptVisitor() {
        var predicate = IsDistinctFromPredicate.of(col("a"), lit(10), false);
        
        var visitor = new TestVisitor();
        var result = predicate.accept(visitor);
        
        assertTrue(result);
        assertEquals(predicate, visitor.visitedPredicate);
    }

    @Test
    void testImplRecord() {
        var lhs = col("test");
        var rhs = lit("value");
        var impl = new IsDistinctFromPredicate.Impl(lhs, rhs, true);

        assertEquals(lhs, impl.lhs());
        assertEquals(rhs, impl.rhs());
        assertTrue(impl.negated());
        assertInstanceOf(IsDistinctFromPredicate.class, impl);
    }

    @Test
    void testWithNullOperands() {
        // Testing with null literal value (valid literal)
        var predicate = IsDistinctFromPredicate.of(col("x"), lit(null), false);
        assertNotNull(predicate);
        assertNull(((LiteralExpr) predicate.rhs()).value());
    }

    @Test
    void testToString() {
        var predicate = IsDistinctFromPredicate.of(col("a"), lit(10), false);
        var str = predicate.toString();
        
        assertNotNull(str);
        assertTrue(str.contains("lhs"));
        assertTrue(str.contains("rhs"));
        assertTrue(str.contains("negated"));
    }

    @Test
    void testIsPredicateSubtype() {
        var predicate = IsDistinctFromPredicate.of(col("a"), lit(1), false);
        
        assertInstanceOf(Predicate.class, predicate);
        assertInstanceOf(Expression.class, predicate);
        assertInstanceOf(Node.class, predicate);
    }

    @Test
    void testCompositionWithOtherPredicates() {
        var pred1 = IsDistinctFromPredicate.of(col("a"), lit(1), false);
        var pred2 = col("b").eq(lit(2));
        
        var andPred = pred1.and(pred2);
        assertNotNull(andPred);
        assertSame(pred1, andPred.lhs());
        assertSame(pred2, andPred.rhs());
        
        var orPred = pred1.or(pred2);
        assertNotNull(orPred);
        assertSame(pred1, orPred.lhs());
        assertSame(pred2, orPred.rhs());
    }

    @Test
    void testNegation() {
        var pred = IsDistinctFromPredicate.of(col("x"), lit(5), false);
        var notPred = pred.not();
        
        assertNotNull(notPred);
        assertSame(pred, notPred.inner());
    }

    // Helper test visitor
    private static class TestVisitor extends io.sqm.core.walk.RecursiveNodeVisitor<Boolean> {
        IsDistinctFromPredicate visitedPredicate;

        @Override
        protected Boolean defaultResult() {
            return false;
        }

        @Override
        public Boolean visitIsDistinctFromPredicate(IsDistinctFromPredicate p) {
            visitedPredicate = p;
            return true;
        }
    }
}
