package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModArithmeticExprTest {

    @Test
    void of() {
        var lhs = Expression.literal(10);
        var rhs = Expression.literal(3);
        var expr = ModArithmeticExpr.of(lhs, rhs);
        
        assertNotNull(expr);
        assertInstanceOf(ModArithmeticExpr.class, expr);
        assertEquals(lhs, expr.lhs());
        assertEquals(rhs, expr.rhs());
    }

    @Test
    void lhs() {
        var lhs = Expression.column("value");
        var expr = ModArithmeticExpr.of(lhs, Expression.literal(2));
        assertEquals(lhs, expr.lhs());
    }

    @Test
    void rhs() {
        var rhs = Expression.column("divisor");
        var expr = ModArithmeticExpr.of(Expression.literal(10), rhs);
        assertEquals(rhs, expr.rhs());
    }

    @Test
    void accept() {
        var expr = ModArithmeticExpr.of(Expression.literal(10), Expression.literal(3));
        var visitor = new TestVisitor();
        var result = expr.accept(visitor);
        assertTrue(result);
    }

    @Test
    void viaExpressionMod() {
        var expr = Expression.literal(10).mod(Expression.literal(3));
        assertInstanceOf(ModArithmeticExpr.class, expr);
    }

    @Test
    void nestedExpressions() {
        var expr = Expression.column("value")
            .mod(Expression.column("divisor"))
            .mod(Expression.literal(2));
        assertInstanceOf(ModArithmeticExpr.class, expr);
    }

    @Test
    void extendsMultiplicativeArithmeticExpr() {
        var expr = ModArithmeticExpr.of(Expression.literal(1), Expression.literal(2));
        assertInstanceOf(MultiplicativeArithmeticExpr.class, expr);
    }

    static class TestVisitor extends io.sqm.core.walk.RecursiveNodeVisitor<Boolean> {
        @Override
        protected Boolean defaultResult() {
            return false;
        }

        @Override
        public Boolean visitModArithmeticExpr(ModArithmeticExpr node) {
            return true;
        }
    }
}
