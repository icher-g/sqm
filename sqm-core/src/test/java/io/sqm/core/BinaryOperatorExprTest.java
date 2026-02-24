package io.sqm.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static io.sqm.dsl.Dsl.col;

class BinaryOperatorExprTest {

    @Test
    void of() {
        var left = col("payload");
        var right = Expression.literal("user");
        var expr = BinaryOperatorExpr.of(left, "->", right);
        
        assertNotNull(expr);
        assertInstanceOf(BinaryOperatorExpr.class, expr);
        assertEquals(left, expr.left());
        assertEquals("->", expr.operator().text());
        assertEquals(right, expr.right());
    }

    @Test
    void left() {
        var left = col("data");
        var expr = BinaryOperatorExpr.of(left, "@>", Expression.literal("{}"));
        assertEquals(left, expr.left());
    }

    @Test
    void operator() {
        var expr = BinaryOperatorExpr.of(
            col("name"),
            "~*",
            Expression.literal("abc")
        );
        assertEquals("~*", expr.operator().text());
    }

    @Test
    void structuredOperatorNamePreservesPostgresSyntax() {
        var expr = BinaryOperatorExpr.of(
            col("a"),
            OperatorName.operator(QualifiedName.of(Identifier.of("pg_catalog")), "##"),
            col("b")
        );

        assertEquals("OPERATOR(pg_catalog.##)", expr.operator().text());
        assertTrue(expr.operator().operatorKeywordSyntax());
        assertTrue(expr.operator().qualified());
        assertEquals(List.of("pg_catalog"), expr.operator().schemaName().values());
    }

    @Test
    void right() {
        var right = col("other_tags");
        var expr = BinaryOperatorExpr.of(
            col("tags"),
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
        var expr = col("payload").op("->", Expression.literal("user"));
        assertInstanceOf(BinaryOperatorExpr.class, expr);
        assertEquals("->", expr.operator().text());
    }

    @Test
    void postgresqlJsonOperators() {
        var payload = col("payload");
        
        var arrow = BinaryOperatorExpr.of(payload, "->", Expression.literal("key"));
        assertEquals("->", arrow.operator().text());
        
        var arrowArrow = BinaryOperatorExpr.of(payload, "->>", Expression.literal("key"));
        assertEquals("->>", arrowArrow.operator().text());
    }

    @Test
    void postgresqlContainsOperator() {
        var expr = BinaryOperatorExpr.of(
            col("data"),
            "@>",
            Expression.literal("{\"a\":1}")
        );
        assertEquals("@>", expr.operator().text());
    }

    @Test
    void postgresqlRegexOperators() {
        var name = col("name");
        
        var tilde = BinaryOperatorExpr.of(name, "~", Expression.literal("abc"));
        assertEquals("~", tilde.operator().text());
        
        var tildeStar = BinaryOperatorExpr.of(name, "~*", Expression.literal("abc"));
        assertEquals("~*", tildeStar.operator().text());
    }

    @Test
    void postgresqlArrayOperators() {
        var tags = col("tags");
        var otherTags = col("other_tags");
        
        var overlap = BinaryOperatorExpr.of(tags, "&&", otherTags);
        assertEquals("&&", overlap.operator().text());
    }

    @Test
    void nestedExpressions() {
        var inner = BinaryOperatorExpr.of(
            col("a"),
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
            BinaryOperatorExpr.of(Expression.literal(1), (String) null, Expression.literal(2))
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
        var left = col("x");
        var right = col("y");
        
        var operators = new String[]{"+", "-", "*", "/", "%", "->", "->>", "@>", "<@", "&&", "||", "~", "~*", "!~", "!~*"};
        
        for (var op : operators) {
            var expr = BinaryOperatorExpr.of(left, op, right);
            assertEquals(op, expr.operator().text());
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
        var left = col("a");
        var right = col("b");
        var op = "->";
        
        var expr1 = BinaryOperatorExpr.of(left, op, right);
        var expr2 = BinaryOperatorExpr.of(left, op, right);
        
        assertEquals(expr1, expr2);
        assertEquals(expr1.hashCode(), expr2.hashCode());
    }

    @Test
    void recordInequality() {
        var expr1 = BinaryOperatorExpr.of(
            col("a"),
            "->",
            col("b")
        );
        var expr2 = BinaryOperatorExpr.of(
            col("a"),
            "->>",
            col("b")
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

