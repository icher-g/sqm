package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static io.sqm.dsl.Dsl.col;

class NegativeArithmeticExprTest {

    @Test
    void of() {
        var expr = Expression.literal(5);
        var negExpr = NegativeArithmeticExpr.of(expr);
        
        assertNotNull(negExpr);
        assertInstanceOf(NegativeArithmeticExpr.class, negExpr);
        assertEquals(expr, negExpr.expr());
    }

    @Test
    void expr() {
        var expr = col("price");
        var negExpr = NegativeArithmeticExpr.of(expr);
        assertEquals(expr, negExpr.expr());
    }

    @Test
    void accept() {
        var negExpr = NegativeArithmeticExpr.of(Expression.literal(5));
        var visitor = new TestVisitor();
        var result = negExpr.accept(visitor);
        assertTrue(result);
    }

    @Test
    void viaExpressionNeg() {
        var expr = Expression.literal(5).neg();
        assertInstanceOf(NegativeArithmeticExpr.class, expr);
    }

    @Test
    void withColumn() {
        var negExpr = NegativeArithmeticExpr.of(col("amount"));
        assertInstanceOf(ColumnExpr.class, negExpr.expr());
    }

    @Test
    void withLiteral() {
        var negExpr = NegativeArithmeticExpr.of(Expression.literal(42));
        assertInstanceOf(LiteralExpr.class, negExpr.expr());
    }

    @Test
    void withNestedExpression() {
        var nested = Expression.literal(5).add(Expression.literal(3));
        var negExpr = NegativeArithmeticExpr.of(nested);
        assertInstanceOf(AddArithmeticExpr.class, negExpr.expr());
    }

    @Test
    void extendsArithmeticExpr() {
        var negExpr = NegativeArithmeticExpr.of(Expression.literal(1));
        assertInstanceOf(ArithmeticExpr.class, negExpr);
    }

    static class TestVisitor extends io.sqm.core.walk.RecursiveNodeVisitor<Boolean> {
        @Override
        protected Boolean defaultResult() {
            return false;
        }

        @Override
        public Boolean visitNegativeArithmeticExpr(NegativeArithmeticExpr node) {
            return true;
        }
    }
}

