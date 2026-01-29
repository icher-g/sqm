package io.sqm.parser.postgresql;

import io.sqm.core.*;
import io.sqm.parser.postgresql.spi.PostgresSpecs;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Parser tests for PostgreSQL array expressions.
 *
 * <p>These tests cover:</p>
 * <ul>
 *   <li>Array constructor: {@code ARRAY[...]} mapped to {@link ArrayExpr}</li>
 *   <li>Array subscripting: {@code expr[index]} mapped to {@link ArraySubscriptExpr}</li>
 *   <li>Array slicing: {@code expr[from:to]} mapped to {@link ArraySliceExpr}</li>
 *   <li>Composition and chaining</li>
 * </ul>
 */
class ArrayExpressionParserTest {

    @Test
    void parsesArrayConstructor() {
        var e = parseExpr("ARRAY[1, 2, 3]");
        var arr = assertInstanceOf(ArrayExpr.class, e);

        assertEquals(3, arr.elements().size());
    }

    @Test
    void parsesArrayConstructor_nested() {
        var e = parseExpr("ARRAY[ARRAY[1,2], ARRAY[3,4]]");
        var outer = assertInstanceOf(ArrayExpr.class, e);

        assertEquals(2, outer.elements().size());
        assertInstanceOf(ArrayExpr.class, outer.elements().get(0));
        assertInstanceOf(ArrayExpr.class, outer.elements().get(1));
    }

    @Test
    void parsesArrayConstructor_nestedBrackets() {
        var e = parseExpr("ARRAY[[1,2],[3,4]]");
        var outer = assertInstanceOf(ArrayExpr.class, e);

        assertEquals(2, outer.elements().size());
        assertInstanceOf(ArrayExpr.class, outer.elements().get(0));
        assertInstanceOf(ArrayExpr.class, outer.elements().get(1));
    }

    @Test
    void parsesArraySubscript_onColumn() {
        var e = parseExpr("arr[1]");
        var sub = assertInstanceOf(ArraySubscriptExpr.class, e);

        assertInstanceOf(ColumnExpr.class, sub.base());
        assertInstanceOf(LiteralExpr.class, sub.index());
    }

    @Test
    void parsesArraySubscript_chained() {
        var e = parseExpr("arr[1][2]");
        var sub2 = assertInstanceOf(ArraySubscriptExpr.class, e);
        var sub1 = assertInstanceOf(ArraySubscriptExpr.class, sub2.base());

        assertInstanceOf(ColumnExpr.class, sub1.base());
    }

    @Test
    void parsesArraySlice_fullBounds() {
        var e = parseExpr("arr[2:5]");
        var slice = assertInstanceOf(ArraySliceExpr.class, e);

        assertTrue(slice.from().isPresent());
        assertTrue(slice.to().isPresent());
    }

    @Test
    void parsesArraySlice_missingFrom() {
        var e = parseExpr("arr[:5]");
        var slice = assertInstanceOf(ArraySliceExpr.class, e);

        assertTrue(slice.from().isEmpty());
        assertTrue(slice.to().isPresent());
    }

    @Test
    void parsesArraySlice_missingTo() {
        var e = parseExpr("arr[2:]");
        var slice = assertInstanceOf(ArraySliceExpr.class, e);

        assertTrue(slice.from().isPresent());
        assertTrue(slice.to().isEmpty());
    }

    @Test
    void parsesArraySlice_missingBoth() {
        var e = parseExpr("arr[:]");
        var slice = assertInstanceOf(ArraySliceExpr.class, e);

        assertTrue(slice.from().isEmpty());
        assertTrue(slice.to().isEmpty());
    }

    @Test
    void parsesArrayConstructor_thenSubscript() {
        var e = parseExpr("ARRAY[1,2,3][2]");
        var sub = assertInstanceOf(ArraySubscriptExpr.class, e);

        assertInstanceOf(ArrayExpr.class, sub.base());
        assertInstanceOf(LiteralExpr.class, sub.index());
    }

    @Test
    void parsesSlice_thenSubscript() {
        var e = parseExpr("arr[1:3][2]");
        var sub = assertInstanceOf(ArraySubscriptExpr.class, e);

        assertInstanceOf(ArraySliceExpr.class, sub.base());
    }

    @Test
    void rejectsEmptySubscript() {
        assertParseError("arr[]");
    }

    @Test
    void rejectsUnclosedSubscript() {
        assertParseError("arr[1");
    }

    @Test
    void rejectsUnclosedSlice() {
        assertParseError("arr[1:2");
    }

    @Test
    void rejectsEmptyArrayConstructor() {
        assertParseError("ARRAY[]");
    }

    @Test
    void rejectsArrayConstructorTrailingComma() {
        assertParseError("ARRAY[1,2,]");
    }

    @Test
    void rejectsArrayConstructorMissingClosingBracket() {
        assertParseError("ARRAY[1,2");
    }

    // ---------------------------------------------------------------------
    // Harness hooks (wire to your existing PostgreSQL parser entry points)
    // ---------------------------------------------------------------------

    private Expression parseExpr(String sql) {
        var ctx = ParseContext.of(new PostgresSpecs());
        var result = ctx.parse(Expression.class, sql);
        return result.value();
    }

    private void assertParseError(String sql) {
        var ctx = ParseContext.of(new PostgresSpecs());
        var result = ctx.parse(Expression.class, sql);
        if (!result.isError()) {
            fail("Expected parse error for: " + sql);
        }
    }
}
