package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IsNullPredicateTest {

    @Test
    void of() {
        var expr = Expression.column("name");
        var predicate = IsNullPredicate.of(expr, false);
        
        assertNotNull(predicate);
        assertInstanceOf(IsNullPredicate.class, predicate);
        assertEquals(expr, predicate.expr());
        assertFalse(predicate.negated());
    }

    @Test
    void isNull() {
        var predicate = IsNullPredicate.of(Expression.column("email"), false);
        assertFalse(predicate.negated());
    }

    @Test
    void isNotNull() {
        var predicate = IsNullPredicate.of(Expression.column("email"), true);
        assertTrue(predicate.negated());
    }

    @Test
    void expr() {
        var expr = Expression.column("status");
        var predicate = IsNullPredicate.of(expr, false);
        assertEquals(expr, predicate.expr());
    }

    @Test
    void accept() {
        var predicate = IsNullPredicate.of(Expression.column("x"), false);
        var visitor = new TestVisitor();
        var result = predicate.accept(visitor);
        assertTrue(result);
    }

    @Test
    void differentExpressions() {
        var expr1 = Expression.column("col1");
        var expr2 = Expression.column("col2");
        
        var pred1 = IsNullPredicate.of(expr1, false);
        var pred2 = IsNullPredicate.of(expr2, false);
        
        assertEquals(expr1, pred1.expr());
        assertEquals(expr2, pred2.expr());
    }

    @Test
    void negatedFlag() {
        var pred1 = IsNullPredicate.of(Expression.column("x"), false);
        var pred2 = IsNullPredicate.of(Expression.column("x"), true);
        
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
