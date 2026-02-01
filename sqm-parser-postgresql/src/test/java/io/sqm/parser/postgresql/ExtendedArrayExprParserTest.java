package io.sqm.parser.postgresql;

import io.sqm.core.ArrayExpr;
import io.sqm.core.ColumnExpr;
import io.sqm.core.Expression;
import io.sqm.core.LiteralExpr;
import io.sqm.parser.ansi.ArrayExprParser;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ArrayExprParser}.
 */
@DisplayName("ANSI ArrayExprParser Tests")
class ExtendedArrayExprParserTest {

    private ParseContext ctx;
    private ArrayExprParser parser;

    @BeforeEach
    void setUp() {
        ctx = ParseContext.of(new io.sqm.parser.postgresql.spi.PostgresSpecs());
        parser = new ArrayExprParser();
    }

    @Test
    @DisplayName("Parse simple 1D array literal with numbers")
    void parseSimple1DArrayWithNumbers() {
        var result = ctx.parse(ArrayExpr.class, "ARRAY[1, 2, 3]");

        assertTrue(result.ok());
        var array = result.value();
        assertNotNull(array);
        assertEquals(3, array.elements().size());

        for (int i = 0; i < 3; i++) {
            var elem = assertInstanceOf(LiteralExpr.class, array.elements().get(i));
            assertEquals((long) (i + 1), elem.value());
        }
    }

    @Test
    @DisplayName("Parse empty array literal")
    void parseEmptyArray() {
        var result = ctx.parse(ArrayExpr.class, "ARRAY[]");

        assertTrue(result.ok());
        var array = result.value();
        assertNotNull(array);
        assertTrue(array.elements().isEmpty());
    }

    @Test
    @DisplayName("Parse 1D array with column expressions")
    void parse1DArrayWithColumns() {
        var result = ctx.parse(ArrayExpr.class, "ARRAY[col1, col2, col3]");

        assertTrue(result.ok());
        var array = result.value();
        assertNotNull(array);
        assertEquals(3, array.elements().size());

        assertInstanceOf(ColumnExpr.class, array.elements().get(0));
        assertInstanceOf(ColumnExpr.class, array.elements().get(1));
        assertInstanceOf(ColumnExpr.class, array.elements().get(2));
    }

    @Test
    @DisplayName("Parse 1D array with string literals")
    void parse1DArrayWithStrings() {
        var result = ctx.parse(ArrayExpr.class, "ARRAY['a', 'b', 'c']");

        assertTrue(result.ok());
        var array = result.value();
        assertNotNull(array);
        assertEquals(3, array.elements().size());

        for (Expression elem : array.elements()) {
            assertInstanceOf(LiteralExpr.class, elem);
        }
    }

    @Test
    @DisplayName("Parse 2D array (multidimensional)")
    void parse2DArray() {
        var result = ctx.parse(ArrayExpr.class, "ARRAY[ARRAY[1, 2], ARRAY[3, 4]]");

        assertTrue(result.ok());
        var array = result.value();
        assertNotNull(array);
        assertEquals(2, array.elements().size());

        var nested1 = assertInstanceOf(ArrayExpr.class, array.elements().get(0));
        assertEquals(2, nested1.elements().size());

        var nested2 = assertInstanceOf(ArrayExpr.class, array.elements().get(1));
        assertEquals(2, nested2.elements().size());
    }

    @Test
    @DisplayName("Parse 3D array")
    void parse3DArray() {
        var result = ctx.parse(ArrayExpr.class, "ARRAY[ARRAY[ARRAY[1]]]");

        assertTrue(result.ok());
        var array = result.value();
        assertNotNull(array);
        assertEquals(1, array.elements().size());

        var level2 = assertInstanceOf(ArrayExpr.class, array.elements().getFirst());
        assertEquals(1, level2.elements().size());

        var level3 = assertInstanceOf(ArrayExpr.class, level2.elements().getFirst());
        assertEquals(1, level3.elements().size());
    }

    @Test
    @DisplayName("Parse array with single element")
    void parseArrayWithSingleElement() {
        var result = ctx.parse(ArrayExpr.class, "ARRAY[42]");

        assertTrue(result.ok());
        var array = result.value();
        assertNotNull(array);
        assertEquals(1, array.elements().size());

        var elem = assertInstanceOf(LiteralExpr.class, array.elements().getFirst());
        assertEquals(42L, elem.value());
    }

    @Test
    @DisplayName("Parse array with mixed expressions")
    void parseArrayWithMixedExpressions() {
        var result = ctx.parse(ArrayExpr.class, "ARRAY[1, col1, 'text']");

        assertTrue(result.ok());
        var array = result.value();
        assertNotNull(array);
        assertEquals(3, array.elements().size());

        assertInstanceOf(LiteralExpr.class, array.elements().get(0));
        assertInstanceOf(ColumnExpr.class, array.elements().get(1));
        assertInstanceOf(LiteralExpr.class, array.elements().get(2));
    }

    @Test
    @DisplayName("Parse array without ARRAY keyword fails")
    void parseWithoutArrayKeywordFails() {
        var result = ctx.parse(ArrayExpr.class, "[1, 2, 3]");

        assertFalse(result.ok());
        assertNotNull(result.errorMessage());
    }

    @Test
    @DisplayName("Parse ARRAY without brackets fails")
    void parseWithoutBracketsFails() {
        var result = ctx.parse(ArrayExpr.class, "ARRAY");

        assertFalse(result.ok());
        assertNotNull(result.errorMessage());
    }

    @Test
    @DisplayName("Parse ARRAY with unclosed bracket fails")
    void parseWithUnclosedBracketFails() {
        var result = ctx.parse(ArrayExpr.class, "ARRAY[1, 2");

        assertFalse(result.ok());
        assertNotNull(result.errorMessage());
    }

    @Test
    @DisplayName("Parser match method returns true for ARRAY keyword")
    void matchMethodReturnsTrueForArrayKeyword() {
        var cur = io.sqm.parser.core.Cursor.of("ARRAY[1]", ctx.identifierQuoting());
        assertTrue(parser.match(cur, ctx));
    }

    @Test
    @DisplayName("Parser match method returns false for non-ARRAY keyword")
    void matchMethodReturnsFalseForNonArrayKeyword() {
        var cur = io.sqm.parser.core.Cursor.of("SELECT", ctx.identifierQuoting());
        assertFalse(parser.match(cur, ctx));
    }

    @Test
    @DisplayName("Parse 2D array with empty nested array")
    void parse2DArrayWithEmptyNested() {
        var result = ctx.parse(ArrayExpr.class, "ARRAY[ARRAY[], ARRAY[1]]");

        assertTrue(result.ok());
        var array = result.value();
        assertNotNull(array);
        assertEquals(2, array.elements().size());

        var nested1 = assertInstanceOf(ArrayExpr.class, array.elements().get(0));
        assertTrue(nested1.elements().isEmpty());

        var nested2 = assertInstanceOf(ArrayExpr.class, array.elements().get(1));
        assertEquals(1, nested2.elements().size());
    }

    @Test
    @DisplayName("Target type is ArrayExpr")
    void targetTypeIsArrayExpr() {
        assertEquals(ArrayExpr.class, parser.targetType());
    }
}
