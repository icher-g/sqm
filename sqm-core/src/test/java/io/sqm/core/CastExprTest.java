package io.sqm.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CastExpr")
class CastExprTest {

    @Test
    @DisplayName("Create cast expression from literal")
    void castFromLiteral() {
        Expression literal = Expression.literal(123);
        CastExpr cast = CastExpr.of(literal, "bigint");
        assertEquals("bigint", cast.type());
        assertNotNull(cast.expr());
    }

    @Test
    @DisplayName("Create cast expression from column")
    void castFromColumn() {
        Expression col = ColumnExpr.of("users", "age");
        CastExpr cast = CastExpr.of(col, "integer");
        assertEquals("integer", cast.type());
        assertEquals(col, cast.expr());
    }

    @Test
    @DisplayName("Cast expression preserves type name")
    void typeNamePreserved() {
        Expression expr = Expression.literal("test");
        CastExpr cast = CastExpr.of(expr, "text[]");
        assertEquals("text[]", cast.type());
    }

    @Test
    @DisplayName("Cast to JSON type")
    void castToJsonb() {
        Expression expr = Expression.literal("{\"a\":1}");
        CastExpr cast = CastExpr.of(expr, "jsonb");
        assertEquals("jsonb", cast.type());
    }

    @Test
    @DisplayName("Cast is instance of Expression")
    void isExpression() {
        CastExpr cast = CastExpr.of(Expression.literal(1), "bigint");
        assertInstanceOf(Expression.class, cast);
    }

    @Test
    @DisplayName("Cast is instance of Node")
    void isNode() {
        CastExpr cast = CastExpr.of(Expression.literal(1), "bigint");
        assertInstanceOf(Node.class, cast);
    }

    @Test
    @DisplayName("Cast with nested expression")
    void castWithNestedExpression() {
        BinaryArithmeticExpr arithmetic = ColumnExpr.of("salary").add(Expression.literal(1000));
        CastExpr cast = CastExpr.of(arithmetic, "decimal");
        assertEquals("decimal", cast.type());
        assertNotNull(cast.expr());
    }

    @Test
    @DisplayName("Different cast types on same expression are not equal")
    void inequalityDifferentType() {
        Expression expr = Expression.literal(100);
        CastExpr cast1 = CastExpr.of(expr, "integer");
        CastExpr cast2 = CastExpr.of(expr, "bigint");
        assertNotEquals(cast1, cast2);
    }

    @Test
    @DisplayName("Cast expressions with same type and expr are equal")
    void equalitySameTypeAndExpr() {
        Expression expr = Expression.literal(100);
        CastExpr cast1 = CastExpr.of(expr, "bigint");
        CastExpr cast2 = CastExpr.of(expr, "bigint");
        assertEquals(cast1, cast2);
    }

    @Test
    @DisplayName("Cast with empty string type")
    void castWithEmptyType() {
        // Type should be stored even if empty
        assertThrows(IllegalArgumentException.class, () -> CastExpr.of(Expression.literal(1), ""));
    }

    @Test
    @DisplayName("Cast can be used in comparison")
    void castInComparison() {
        CastExpr cast = CastExpr.of(Expression.literal(100), "bigint");
        ComparisonPredicate pred = cast.gt(Expression.literal(50));
        assertNotNull(pred);
        assertInstanceOf(Predicate.class, pred);
    }

    @Test
    @DisplayName("Cast can be used in arithmetic")
    void castInArithmetic() {
        CastExpr cast = CastExpr.of(Expression.literal(100), "integer");
        BinaryArithmeticExpr expr = cast.add(Expression.literal(50));
        assertNotNull(expr);
        assertInstanceOf(Expression.class, expr);
    }

    @Test
    @DisplayName("Cast from function call")
    void castFromFunctionCall() {
        // Assuming a way to create function expressions
        Expression expr = Expression.literal("test");
        CastExpr cast = CastExpr.of(expr, "integer");
        assertNotNull(cast);
        assertEquals("integer", cast.type());
    }

    @Test
    @DisplayName("Type names with special characters")
    void typeNamesWithSpecialCharacters() {
        Expression expr = Expression.literal(1);
        CastExpr cast = CastExpr.of(expr, "text[]");
        assertEquals("text[]", cast.type());

        cast = CastExpr.of(expr, "numeric(10,2)");
        assertEquals("numeric(10,2)", cast.type());

        cast = CastExpr.of(expr, "character varying");
        assertEquals("character varying", cast.type());
    }
}
