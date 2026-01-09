package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DivArithmeticExprTest {

    @Test
    void of() {
        var lhs = Expression.literal(10);
        var rhs = Expression.literal(2);
        var expr = DivArithmeticExpr.of(lhs, rhs);
        
        assertNotNull(expr);
        assertInstanceOf(DivArithmeticExpr.class, expr);
        assertEquals(lhs, expr.lhs());
        assertEquals(rhs, expr.rhs());
    }

    @Test
    void lhs() {
        var lhs = Expression.column("total");
        var expr = DivArithmeticExpr.of(lhs, Expression.literal(2));
        assertEquals(lhs, expr.lhs());
    }

    @Test
    void rhs() {
        var rhs = Expression.column("count");
        var expr = DivArithmeticExpr.of(Expression.literal(100), rhs);
        assertEquals(rhs, expr.rhs());
    }

    @Test
    void accept() {
        var expr = DivArithmeticExpr.of(Expression.literal(10), Expression.literal(2));
        var visitor = new TestVisitor();
        var result = expr.accept(visitor);
        assertTrue(result);
    }

    @Test
    void viaExpressionDiv() {
        var expr = Expression.literal(10).div(Expression.literal(2));
        assertInstanceOf(DivArithmeticExpr.class, expr);
    }

    @Test
    void nestedExpressions() {
        var expr = Expression.column("total")
            .div(Expression.column("count"))
            .div(Expression.literal(2));
        assertInstanceOf(DivArithmeticExpr.class, expr);
    }

    @Test
    void extendsMultiplicativeArithmeticExpr() {
        var expr = DivArithmeticExpr.of(Expression.literal(1), Expression.literal(2));
        assertInstanceOf(MultiplicativeArithmeticExpr.class, expr);
    }

    static class TestVisitor extends io.sqm.core.walk.RecursiveNodeVisitor<Boolean> {
        @Override
        protected Boolean defaultResult() {
            return false;
        }

        @Override
        public Boolean visitDivArithmeticExpr(DivArithmeticExpr node) {
            return true;
        }
    }
}
