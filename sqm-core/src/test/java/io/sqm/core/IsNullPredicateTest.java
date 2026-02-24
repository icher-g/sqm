package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static io.sqm.dsl.Dsl.col;

class IsNullPredicateTest {

    @Test
    void of() {
        var expr = col("name");
        var predicate = IsNullPredicate.of(expr, false);
        
        assertNotNull(predicate);
        assertInstanceOf(IsNullPredicate.class, predicate);
        assertEquals(expr, predicate.expr());
        assertFalse(predicate.negated());
    }

    @Test
    void isNull() {
        var predicate = IsNullPredicate.of(col("email"), false);
        assertFalse(predicate.negated());
    }

    @Test
    void isNotNull() {
        var predicate = IsNullPredicate.of(col("email"), true);
        assertTrue(predicate.negated());
    }

    @Test
    void expr() {
        var expr = col("status");
        var predicate = IsNullPredicate.of(expr, false);
        assertEquals(expr, predicate.expr());
    }

    @Test
    void accept() {
        var predicate = IsNullPredicate.of(col("x"), false);
        var visitor = new TestVisitor();
        var result = predicate.accept(visitor);
        assertTrue(result);
    }

    @Test
    void differentExpressions() {
        var expr1 = col("col1");
        var expr2 = col("col2");
        
        var pred1 = IsNullPredicate.of(expr1, false);
        var pred2 = IsNullPredicate.of(expr2, false);
        
        assertEquals(expr1, pred1.expr());
        assertEquals(expr2, pred2.expr());
    }

    @Test
    void negatedFlag() {
        var pred1 = IsNullPredicate.of(col("x"), false);
        var pred2 = IsNullPredicate.of(col("x"), true);
        
        assertFalse(pred1.negated());
        assertTrue(pred2.negated());
    }

    static class TestVisitor extends io.sqm.core.walk.RecursiveNodeVisitor<Boolean> {
        @Override
        protected Boolean defaultResult() {
            return false;
        }

        @Override
        public Boolean visitIsNullPredicate(IsNullPredicate node) {
            return true;
        }
    }
}

