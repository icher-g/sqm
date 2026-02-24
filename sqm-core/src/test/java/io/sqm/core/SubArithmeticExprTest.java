package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static io.sqm.dsl.Dsl.col;

class SubArithmeticExprTest {

    @Test
    void of() {
        var lhs = Expression.literal(10);
        var rhs = Expression.literal(3);
        var expr = SubArithmeticExpr.of(lhs, rhs);
        
        assertNotNull(expr);
        assertInstanceOf(SubArithmeticExpr.class, expr);
        assertEquals(lhs, expr.lhs());
        assertEquals(rhs, expr.rhs());
    }

    @Test
    void lhs() {
        var lhs = col("total");
        var expr = SubArithmeticExpr.of(lhs, Expression.literal(5));
        assertEquals(lhs, expr.lhs());
    }

    @Test
    void rhs() {
        var rhs = col("discount");
        var expr = SubArithmeticExpr.of(Expression.literal(100), rhs);
        assertEquals(rhs, expr.rhs());
    }

    @Test
    void accept() {
        var expr = SubArithmeticExpr.of(Expression.literal(10), Expression.literal(3));
        var visitor = new TestVisitor();
        var result = expr.accept(visitor);
        assertTrue(result);
    }

    @Test
    void viaExpressionSub() {
        var expr = Expression.literal(10).sub(Expression.literal(3));
        assertInstanceOf(SubArithmeticExpr.class, expr);
    }

    @Test
    void nestedExpressions() {
        var expr = col("total")
            .sub(col("discount"))
            .sub(Expression.literal(5));
        assertInstanceOf(SubArithmeticExpr.class, expr);
    }

    @Test
    void extendsAdditiveArithmeticExpr() {
        var expr = SubArithmeticExpr.of(Expression.literal(1), Expression.literal(2));
        assertInstanceOf(AdditiveArithmeticExpr.class, expr);
    }

    static class TestVisitor extends io.sqm.core.walk.RecursiveNodeVisitor<Boolean> {
        @Override
        protected Boolean defaultResult() {
            return false;
        }

        @Override
        public Boolean visitSubArithmeticExpr(SubArithmeticExpr node) {
            return true;
        }
    }
}

