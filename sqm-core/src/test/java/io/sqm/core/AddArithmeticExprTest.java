package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static io.sqm.dsl.Dsl.col;

class AddArithmeticExprTest {

    @Test
    void of() {
        var lhs = Expression.literal(5);
        var rhs = Expression.literal(3);
        var expr = AddArithmeticExpr.of(lhs, rhs);
        
        assertNotNull(expr);
        assertInstanceOf(AddArithmeticExpr.class, expr);
        assertEquals(lhs, expr.lhs());
        assertEquals(rhs, expr.rhs());
    }

    @Test
    void lhs() {
        var lhs = col("price");
        var expr = AddArithmeticExpr.of(lhs, Expression.literal(10));
        assertEquals(lhs, expr.lhs());
    }

    @Test
    void rhs() {
        var rhs = col("discount");
        var expr = AddArithmeticExpr.of(Expression.literal(100), rhs);
        assertEquals(rhs, expr.rhs());
    }

    @Test
    void accept() {
        var expr = AddArithmeticExpr.of(Expression.literal(1), Expression.literal(2));
        var visitor = new TestVisitor();
        var result = expr.accept(visitor);
        assertTrue(result);
    }

    @Test
    void matchArithmetic() {
        var expr = AddArithmeticExpr.of(Expression.literal(1), Expression.literal(2));
        var match = expr.matchArithmetic();
        assertNotNull(match);
    }

    @Test
    void viaExpressionAdd() {
        var expr = Expression.literal(5).add(Expression.literal(3));
        assertInstanceOf(AddArithmeticExpr.class, expr);
    }

    @Test
    void nestedExpressions() {
        var expr = col("a")
            .add(col("b"))
            .add(Expression.literal(10));
        assertInstanceOf(AddArithmeticExpr.class, expr);
    }

    static class TestVisitor extends io.sqm.core.walk.RecursiveNodeVisitor<Boolean> {
        @Override
        protected Boolean defaultResult() {
            return false;
        }

        @Override
        public Boolean visitAddArithmeticExpr(AddArithmeticExpr node) {
            return true;
        }
    }
}

