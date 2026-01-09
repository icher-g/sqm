package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MulArithmeticExprTest {

    @Test
    void of() {
        var lhs = Expression.literal(5);
        var rhs = Expression.literal(3);
        var expr = MulArithmeticExpr.of(lhs, rhs);
        
        assertNotNull(expr);
        assertInstanceOf(MulArithmeticExpr.class, expr);
        assertEquals(lhs, expr.lhs());
        assertEquals(rhs, expr.rhs());
    }

    @Test
    void lhs() {
        var lhs = Expression.column("quantity");
        var expr = MulArithmeticExpr.of(lhs, Expression.literal(2));
        assertEquals(lhs, expr.lhs());
    }

    @Test
    void rhs() {
        var rhs = Expression.column("price");
        var expr = MulArithmeticExpr.of(Expression.literal(10), rhs);
        assertEquals(rhs, expr.rhs());
    }

    @Test
    void accept() {
        var expr = MulArithmeticExpr.of(Expression.literal(5), Expression.literal(3));
        var visitor = new TestVisitor();
        var result = expr.accept(visitor);
        assertTrue(result);
    }

    @Test
    void viaExpressionMul() {
        var expr = Expression.literal(5).mul(Expression.literal(3));
        assertInstanceOf(MulArithmeticExpr.class, expr);
    }

    @Test
    void nestedExpressions() {
        var expr = Expression.column("quantity")
            .mul(Expression.column("price"))
            .mul(Expression.literal(0.9));
        assertInstanceOf(MulArithmeticExpr.class, expr);
    }

    @Test
    void extendsMultiplicativeArithmeticExpr() {
        var expr = MulArithmeticExpr.of(Expression.literal(1), Expression.literal(2));
        assertInstanceOf(MultiplicativeArithmeticExpr.class, expr);
    }

    static class TestVisitor extends io.sqm.core.walk.RecursiveNodeVisitor<Boolean> {
        @Override
        protected Boolean defaultResult() {
            return false;
        }

        @Override
        public Boolean visitMulArithmeticExpr(MulArithmeticExpr node) {
            return true;
        }
    }
}
