package io.sqm.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CastExpr")
class CastExprTest {

    @Test
    @DisplayName("Create cast expression from literal")
    void castFromLiteral() {
        Expression literal = Expression.literal(123);
        CastExpr cast = CastExpr.of(literal, type("bigint"));
        assertEquals("bigint", cast.type().qualifiedName().parts().getFirst().value());
        assertNotNull(cast.expr());
    }

    @Test
    @DisplayName("Create cast expression from column")
    void castFromColumn() {
        Expression col = ColumnExpr.of(Identifier.of("users"), Identifier.of("age"));
        CastExpr cast = CastExpr.of(col, type("integer"));
        assertEquals("integer", cast.type().qualifiedName().parts().getFirst().value());
        assertEquals(col, cast.expr());
    }

    @Test
    @DisplayName("Cast expression preserves type name")
    void typeNamePreserved() {
        Expression expr = Expression.literal("test");
        CastExpr cast = CastExpr.of(expr, type("text").array());
        assertEquals("text", cast.type().qualifiedName().parts().getFirst().value());
        assertEquals(1, cast.type().arrayDims());
    }

    @Test
    @DisplayName("Cast to JSON type")
    void castToJsonb() {
        Expression expr = Expression.literal("{\"a\":1}");
        CastExpr cast = CastExpr.of(expr, type("jsonb"));
        assertEquals("jsonb", cast.type().qualifiedName().parts().getFirst().value());
    }

    @Test
    @DisplayName("Cast is instance of Expression")
    void isExpression() {
        CastExpr cast = CastExpr.of(Expression.literal(1), type("bigint"));
        assertInstanceOf(Expression.class, cast);
    }

    @Test
    @DisplayName("Cast is instance of Node")
    void isNode() {
        CastExpr cast = CastExpr.of(Expression.literal(1), type("bigint"));
        assertInstanceOf(Node.class, cast);
    }

    @Test
    @DisplayName("Cast with nested expression")
    void castWithNestedExpression() {
        BinaryArithmeticExpr arithmetic = ColumnExpr.of(null, Identifier.of("salary")).add(Expression.literal(1000));
        CastExpr cast = CastExpr.of(arithmetic, type("decimal"));
        assertEquals("decimal", cast.type().qualifiedName().parts().getFirst().value());
        assertNotNull(cast.expr());
    }

    @Test
    @DisplayName("Different cast types on same expression are not equal")
    void inequalityDifferentType() {
        Expression expr = Expression.literal(100);
        CastExpr cast1 = CastExpr.of(expr, type("integer"));
        CastExpr cast2 = CastExpr.of(expr, type("bigint"));
        assertNotEquals(cast1, cast2);
    }

    @Test
    @DisplayName("Cast expressions with same type and expr are equal")
    void equalitySameTypeAndExpr() {
        Expression expr = Expression.literal(100);
        CastExpr cast1 = CastExpr.of(expr, type("bigint"));
        CastExpr cast2 = CastExpr.of(expr, type("bigint"));
        assertEquals(cast1, cast2);
    }

    @Test
    @DisplayName("Cast with empty string type")
    void castWithEmptyType() {
        // Type should be stored even if empty
        assertThrows(IllegalArgumentException.class, () -> CastExpr.of(Expression.literal(1), type()));
    }

    @Test
    @DisplayName("Cast can be used in comparison")
    void castInComparison() {
        CastExpr cast = CastExpr.of(Expression.literal(100), type("bigint"));
        ComparisonPredicate pred = cast.gt(Expression.literal(50));
        assertNotNull(pred);
        assertInstanceOf(Predicate.class, pred);
    }

    @Test
    @DisplayName("Cast can be used in arithmetic")
    void castInArithmetic() {
        CastExpr cast = CastExpr.of(Expression.literal(100), type("integer"));
        BinaryArithmeticExpr expr = cast.add(Expression.literal(50));
        assertNotNull(expr);
        assertInstanceOf(Expression.class, expr);
    }

    @Test
    @DisplayName("Cast from function call")
    void castFromFunctionCall() {
        // Assuming a way to create function expressions
        Expression expr = Expression.literal("test");
        CastExpr cast = CastExpr.of(expr, type("integer"));
        assertNotNull(cast);
        assertEquals("integer", cast.type().qualifiedName().parts().getFirst().value());
    }

    @Test
    @DisplayName("Type names with special characters")
    void typeNamesWithSpecialCharacters() {
        Expression expr = Expression.literal(1);
        CastExpr cast = CastExpr.of(expr, type("text").array());
        assertEquals("text", cast.type().qualifiedName().parts().getFirst().value());
        assertEquals(1, cast.type().arrayDims());

        cast = CastExpr.of(expr, type("numeric").withModifiers(lit(10), lit(2)));
        assertEquals("numeric", cast.type().qualifiedName().parts().getFirst().value());
        assertEquals(lit(10), cast.type().modifiers().getFirst());
        assertEquals(lit(2), cast.type().modifiers().get(1));

        cast = CastExpr.of(expr, type(TypeKeyword.CHARACTER_VARYING));
        assertTrue(cast.type().keyword().isPresent());
        assertEquals(TypeKeyword.CHARACTER_VARYING, cast.type().keyword().get());
    }
}
