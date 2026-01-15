package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UnaryOperatorExprTest {

    @Test
    void of() {
        var expr = Expression.column("amount");
        var unaryExpr = UnaryOperatorExpr.of("-", expr);
        
        assertNotNull(unaryExpr);
        assertInstanceOf(UnaryOperatorExpr.class, unaryExpr);
        assertEquals("-", unaryExpr.operator());
        assertEquals(expr, unaryExpr.expr());
    }

    @Test
    void operator() {
        var expr = UnaryOperatorExpr.of("~", Expression.column("mask"));
        assertEquals("~", expr.operator());
    }

    @Test
    void expr() {
        var operand = Expression.column("value");
        var expr = UnaryOperatorExpr.of("-", operand);
        assertEquals(operand, expr.expr());
    }

    @Test
    void accept() {
        var expr = UnaryOperatorExpr.of("-", Expression.literal(5));
        var visitor = new TestVisitor();
        var result = expr.accept(visitor);
        assertTrue(result);
    }

    @Test
    void viaExpressionUnary() {
        var expr = Expression.column("amount").unary("-");
        assertInstanceOf(UnaryOperatorExpr.class, expr);
        assertEquals("-", expr.operator());
    }

    @Test
    void arithmeticNegation() {
        var expr = UnaryOperatorExpr.of("-", Expression.literal(42));
        assertEquals("-", expr.operator());
        assertInstanceOf(LiteralExpr.class, expr.expr());
    }

    @Test
    void bitwiseNot() {
        var expr = UnaryOperatorExpr.of("~", Expression.column("mask"));
        assertEquals("~", expr.operator());
    }

    @Test
    void withColumn() {
        var expr = UnaryOperatorExpr.of("-", Expression.column("price"));
        assertInstanceOf(ColumnExpr.class, expr.expr());
    }

    @Test
    void withLiteral() {
        var expr = UnaryOperatorExpr.of("-", Expression.literal(100));
        assertInstanceOf(LiteralExpr.class, expr.expr());
    }

    @Test
    void withNestedExpression() {
        var nested = Expression.column("a").add(Expression.column("b"));
        var expr = UnaryOperatorExpr.of("-", nested);
        assertInstanceOf(AddArithmeticExpr.class, expr.expr());
    }

    @Test
    void withFunction() {
        var func = Expression.func("abs", Expression.funcArg(Expression.column("x")));
        var expr = UnaryOperatorExpr.of("-", func);
        assertInstanceOf(FunctionExpr.class, expr.expr());
    }

    @Test
    void nullOperatorThrows() {
        assertThrows(NullPointerException.class, () ->
            UnaryOperatorExpr.of(null, Expression.literal(1))
        );
    }

    @Test
    void blankOperatorThrows() {
        assertThrows(IllegalArgumentException.class, () ->
            UnaryOperatorExpr.of("", Expression.literal(1))
        );
        assertThrows(IllegalArgumentException.class, () ->
            UnaryOperatorExpr.of("   ", Expression.literal(1))
        );
    }

    @Test
    void nullExprThrows() {
        assertThrows(NullPointerException.class, () ->
            UnaryOperatorExpr.of("-", null)
        );
    }

    @Test
    void differentOperators() {
        var operand = Expression.column("x");
        var operators = new String[]{"-", "+", "~", "!", "@"};
        
        for (var op : operators) {
            var expr = UnaryOperatorExpr.of(op, operand);
            assertEquals(op, expr.operator());
        }
    }

    @Test
    void multiCharacterOperator() {
        var expr = UnaryOperatorExpr.of("!!", Expression.column("array"));
        assertEquals("!!", expr.operator());
    }

    @Test
    void implementsExpression() {
        var expr = UnaryOperatorExpr.of("-", Expression.literal(1));
        assertInstanceOf(Expression.class, expr);
    }

    @Test
    void recordEquality() {
        var operand = Expression.column("amount");
        var op = "-";
        
        var expr1 = UnaryOperatorExpr.of(op, operand);
        var expr2 = UnaryOperatorExpr.of(op, operand);
        
        assertEquals(expr1, expr2);
        assertEquals(expr1.hashCode(), expr2.hashCode());
    }

    @Test
    void recordInequality() {
        var operand = Expression.column("x");
        var expr1 = UnaryOperatorExpr.of("-", operand);
        var expr2 = UnaryOperatorExpr.of("~", operand);
        
        assertNotEquals(expr1, expr2);
    }

    @Test
    void recordInequalityDifferentOperands() {
        var expr1 = UnaryOperatorExpr.of("-", Expression.column("a"));
        var expr2 = UnaryOperatorExpr.of("-", Expression.column("b"));
        
        assertNotEquals(expr1, expr2);
    }

    @Test
    void nestedUnaryOperators() {
        var inner = UnaryOperatorExpr.of("-", Expression.column("amount"));
        var outer = UnaryOperatorExpr.of("~", inner);
        
        assertInstanceOf(UnaryOperatorExpr.class, outer.expr());
        assertEquals("~", outer.operator());
        assertEquals("-", ((UnaryOperatorExpr) outer.expr()).operator());
    }

    @Test
    void withBinaryOperatorExpr() {
        var binary = BinaryOperatorExpr.of(
            Expression.column("a"),
            "->",
            Expression.literal("b")
        );
        var unary = UnaryOperatorExpr.of("-", binary);
        
        assertInstanceOf(BinaryOperatorExpr.class, unary.expr());
    }

    @Test
    void toStringTest() {
        var expr = UnaryOperatorExpr.of("-", Expression.column("amount"));
        var str = expr.toString();
        assertNotNull(str);
        assertTrue(str.contains("UnaryOperatorExpr") || str.contains("-"));
    }

    static class TestVisitor extends io.sqm.core.walk.RecursiveNodeVisitor<Boolean> {
        @Override
        protected Boolean defaultResult() {
            return false;
        }

        @Override
        public Boolean visitUnaryOperatorExpr(UnaryOperatorExpr node) {
            return true;
        }
    }
}
