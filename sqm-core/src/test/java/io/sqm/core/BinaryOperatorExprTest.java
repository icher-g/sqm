package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BinaryOperatorExprTest {

    @Test
    void of() {
        var left = Expression.column("payload");
        var right = Expression.literal("user");
        var expr = BinaryOperatorExpr.of(left, "->", right);
        
        assertNotNull(expr);
        assertInstanceOf(BinaryOperatorExpr.class, expr);
        assertEquals(left, expr.left());
        assertEquals("->", expr.operator());
        assertEquals(right, expr.right());
    }

    @Test
    void left() {
        var left = Expression.column("data");
        var expr = BinaryOperatorExpr.of(left, "@>", Expression.literal("{}"));
        assertEquals(left, expr.left());
    }

    @Test
    void operator() {
        var expr = BinaryOperatorExpr.of(
            Expression.column("name"),
            "~*",
            Expression.literal("abc")
        );
        assertEquals("~*", expr.operator());
    }

    @Test
    void right() {
        var right = Expression.column("other_tags");
        var expr = BinaryOperatorExpr.of(
            Expression.column("tags"),
            "&&",
            right
        );
        assertEquals(right, expr.right());
    }

    @Test
    void accept() {
        var expr = BinaryOperatorExpr.of(
            Expression.literal(1),
            "+",
            Expression.literal(2)
        );
        var visitor = new TestVisitor();
        var result = expr.accept(visitor);
        assertTrue(result);
    }

    @Test
    void viaExpressionOp() {
        var expr = Expression.column("payload").op("->", Expression.literal("user"));
        assertInstanceOf(BinaryOperatorExpr.class, expr);
        assertEquals("->", expr.operator());
    }

    @Test
    void postgresqlJsonOperators() {
        var payload = Expression.column("payload");
        
        var arrow = BinaryOperatorExpr.of(payload, "->", Expression.literal("key"));
        assertEquals("->", arrow.operator());
        
        var arrowArrow = BinaryOperatorExpr.of(payload, "->>", Expression.literal("key"));
        assertEquals("->>", arrowArrow.operator());
    }

    @Test
    void postgresqlContainsOperator() {
        var expr = BinaryOperatorExpr.of(
            Expression.column("data"),
            "@>",
            Expression.literal("{\"a\":1}")
        );
        assertEquals("@>", expr.operator());
    }

    @Test
    void postgresqlRegexOperators() {
        var name = Expression.column("name");
        
        var tilde = BinaryOperatorExpr.of(name, "~", Expression.literal("abc"));
        assertEquals("~", tilde.operator());
        
        var tildeStar = BinaryOperatorExpr.of(name, "~*", Expression.literal("abc"));
        assertEquals("~*", tildeStar.operator());
    }

    @Test
    void postgresqlArrayOperators() {
        var tags = Expression.column("tags");
        var otherTags = Expression.column("other_tags");
        
        var overlap = BinaryOperatorExpr.of(tags, "&&", otherTags);
        assertEquals("&&", overlap.operator());
    }

    @Test
    void nestedExpressions() {
        var inner = BinaryOperatorExpr.of(
            Expression.column("a"),
            "->",
            Expression.literal("b")
        );
        var outer = BinaryOperatorExpr.of(
            inner,
            "->>",
            Expression.literal("c")
        );
        assertInstanceOf(BinaryOperatorExpr.class, outer.left());
    }

    @Test
    void nullLeftThrows() {
        assertThrows(NullPointerException.class, () ->
            BinaryOperatorExpr.of(null, "->", Expression.literal(1))
        );
    }

    @Test
    void nullOperatorThrows() {
        assertThrows(NullPointerException.class, () ->
            BinaryOperatorExpr.of(Expression.literal(1), null, Expression.literal(2))
        );
    }

    @Test
    void blankOperatorThrows() {
        assertThrows(IllegalArgumentException.class, () ->
            BinaryOperatorExpr.of(Expression.literal(1), "", Expression.literal(2))
        );
        assertThrows(IllegalArgumentException.class, () ->
            BinaryOperatorExpr.of(Expression.literal(1), "   ", Expression.literal(2))
        );
    }

    @Test
    void nullRightThrows() {
        assertThrows(NullPointerException.class, () ->
            BinaryOperatorExpr.of(Expression.literal(1), "->", null)
        );
    }

    @Test
    void differentOperators() {
        var left = Expression.column("x");
        var right = Expression.column("y");
        
        var operators = new String[]{"+", "-", "*", "/", "%", "->", "->>", "@>", "<@", "&&", "||", "~", "~*", "!~", "!~*"};
        
        for (var op : operators) {
            var expr = BinaryOperatorExpr.of(left, op, right);
            assertEquals(op, expr.operator());
        }
    }

    @Test
    void implementsExpression() {
        var expr = BinaryOperatorExpr.of(
            Expression.literal(1),
            "+",
            Expression.literal(2)
        );
        assertInstanceOf(Expression.class, expr);
    }

    @Test
    void recordEquality() {
        var left = Expression.column("a");
        var right = Expression.column("b");
        var op = "->";
        
        var expr1 = BinaryOperatorExpr.of(left, op, right);
        var expr2 = BinaryOperatorExpr.of(left, op, right);
        
        assertEquals(expr1, expr2);
        assertEquals(expr1.hashCode(), expr2.hashCode());
    }

    @Test
    void recordInequality() {
        var expr1 = BinaryOperatorExpr.of(
            Expression.column("a"),
            "->",
            Expression.column("b")
        );
        var expr2 = BinaryOperatorExpr.of(
            Expression.column("a"),
            "->>",
            Expression.column("b")
        );
        
        assertNotEquals(expr1, expr2);
    }

    static class TestVisitor extends io.sqm.core.walk.RecursiveNodeVisitor<Boolean> {
        @Override
        protected Boolean defaultResult() {
            return false;
        }

        @Override
        public Boolean visitBinaryOperatorExpr(BinaryOperatorExpr node) {
            return true;
        }
    }
}
