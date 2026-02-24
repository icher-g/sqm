package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.*;

class UnaryOperatorExprTest {

    @Test
    void of() {
        var expr = col("amount");
        var unaryExpr = UnaryOperatorExpr.of("-", expr);
        
        assertNotNull(unaryExpr);
        assertInstanceOf(UnaryOperatorExpr.class, unaryExpr);
        assertEquals("-", unaryExpr.operator().text());
        assertEquals(expr, unaryExpr.expr());
    }

    @Test
    void operator() {
        var expr = UnaryOperatorExpr.of("~", col("mask"));
        assertEquals("~", expr.operator().text());
    }

    @Test
    void structuredOperatorName() {
        var expr = UnaryOperatorExpr.of(OperatorName.of("~"), col("mask"));
        assertEquals("~", expr.operator().text());
        assertEquals("~", expr.operator().symbol());
        assertFalse(expr.operator().operatorKeywordSyntax());
    }

    @Test
    void expr() {
        var operand = col("value");
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
        var expr = col("amount").unary("-");
        assertInstanceOf(UnaryOperatorExpr.class, expr);
        assertEquals("-", expr.operator().text());
    }

    @Test
    void arithmeticNegation() {
        var expr = UnaryOperatorExpr.of("-", Expression.literal(42));
        assertEquals("-", expr.operator().text());
        assertInstanceOf(LiteralExpr.class, expr.expr());
    }

    @Test
    void bitwiseNot() {
        var expr = UnaryOperatorExpr.of("~", col("mask"));
        assertEquals("~", expr.operator().text());
    }

    @Test
    void withColumn() {
        var expr = UnaryOperatorExpr.of("-", col("price"));
        assertInstanceOf(ColumnExpr.class, expr.expr());
    }

    @Test
    void withLiteral() {
        var expr = UnaryOperatorExpr.of("-", Expression.literal(100));
        assertInstanceOf(LiteralExpr.class, expr.expr());
    }

    @Test
    void withNestedExpression() {
        var nested = col("a").add(col("b"));
        var expr = UnaryOperatorExpr.of("-", nested);
        assertInstanceOf(AddArithmeticExpr.class, expr.expr());
    }

    @Test
    void withFunction() {
        var func = func("abs", Expression.funcArg(col("x")));
        var expr = UnaryOperatorExpr.of("-", func);
        assertInstanceOf(FunctionExpr.class, expr.expr());
    }

    @Test
    void nullOperatorThrows() {
        assertThrows(NullPointerException.class, () ->
            UnaryOperatorExpr.of((String) null, Expression.literal(1))
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
        var operand = col("x");
        var operators = new String[]{"-", "+", "~", "!", "@"};
        
        for (var op : operators) {
            var expr = UnaryOperatorExpr.of(op, operand);
            assertEquals(op, expr.operator().text());
        }
    }

    @Test
    void multiCharacterOperator() {
        var expr = UnaryOperatorExpr.of("!!", col("array"));
        assertEquals("!!", expr.operator().text());
    }

    @Test
    void implementsExpression() {
        var expr = UnaryOperatorExpr.of("-", Expression.literal(1));
        assertInstanceOf(Expression.class, expr);
    }

    @Test
    void recordEquality() {
        var operand = col("amount");
        var op = "-";
        
        var expr1 = UnaryOperatorExpr.of(op, operand);
        var expr2 = UnaryOperatorExpr.of(op, operand);
        
        assertEquals(expr1, expr2);
        assertEquals(expr1.hashCode(), expr2.hashCode());
    }

    @Test
    void recordInequality() {
        var operand = col("x");
        var expr1 = UnaryOperatorExpr.of("-", operand);
        var expr2 = UnaryOperatorExpr.of("~", operand);
        
        assertNotEquals(expr1, expr2);
    }

    @Test
    void recordInequalityDifferentOperands() {
        var expr1 = UnaryOperatorExpr.of("-", col("a"));
        var expr2 = UnaryOperatorExpr.of("-", col("b"));
        
        assertNotEquals(expr1, expr2);
    }

    @Test
    void nestedUnaryOperators() {
        var inner = UnaryOperatorExpr.of("-", col("amount"));
        var outer = UnaryOperatorExpr.of("~", inner);
        
        assertInstanceOf(UnaryOperatorExpr.class, outer.expr());
        assertEquals("~", outer.operator().text());
        assertEquals("-", ((UnaryOperatorExpr) outer.expr()).operator().text());
    }

    @Test
    void withBinaryOperatorExpr() {
        var binary = BinaryOperatorExpr.of(
            col("a"),
            "->",
            Expression.literal("b")
        );
        var unary = UnaryOperatorExpr.of("-", binary);
        
        assertInstanceOf(BinaryOperatorExpr.class, unary.expr());
    }

    @Test
    void toStringTest() {
        var expr = UnaryOperatorExpr.of("-", col("amount"));
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

