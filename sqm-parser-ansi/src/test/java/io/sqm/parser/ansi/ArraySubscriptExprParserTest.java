package io.sqm.parser.ansi;

import io.sqm.core.ArraySubscriptExpr;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ArraySubscriptExprParser} in ANSI dialect.
 *
 * <p>Note: ANSI SQL does not support array subscripts, so all tests should fail.</p>
 */
@DisplayName("ANSI ArraySubscriptExprParser Tests")
class ArraySubscriptExprParserTest {

    private ParseContext ctx;
    private ArraySubscriptExprParser parser;

    @BeforeEach
    void setUp() {
        ctx = ParseContext.of(new AnsiSpecs());
        parser = new ArraySubscriptExprParser();
    }

    @Test
    @DisplayName("Parse array subscript is not supported in ANSI")
    void parseArraySubscriptNotSupported() {
        var result = ctx.parse(ArraySubscriptExpr.class, "arr[1]");

        assertFalse(result.ok());
        assertNotNull(result.errorMessage());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Array subscripts are not supported"));
    }

    @Test
    @DisplayName("Parse array subscript with literal index is not supported")
    void parseArraySubscriptWithLiteralIndexNotSupported() {
        var result = ctx.parse(ArraySubscriptExpr.class, "col[5]");

        assertFalse(result.ok());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Array subscripts are not supported"));
    }

    @Test
    @DisplayName("Parse array subscript with column index is not supported")
    void parseArraySubscriptWithColumnIndexNotSupported() {
        var result = ctx.parse(ArraySubscriptExpr.class, "arr[idx]");

        assertFalse(result.ok());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Array subscripts are not supported"));
    }

    @Test
    @DisplayName("Parse nested array subscript is not supported")
    void parseNestedArraySubscriptNotSupported() {
        var result = ctx.parse(ArraySubscriptExpr.class, "arr[1][2]");

        assertFalse(result.ok());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Array subscripts are not supported"));
    }

    @Test
    @DisplayName("Match method returns false when colon present (slice)")
    void matchMethodReturnsFalseForSlice() {
        var cur = Cursor.of("[1:3]", ctx.identifierQuoting());
        // Should match as false since this is a slice, not subscript
        assertFalse(parser.match(cur, ctx));
    }

    @Test
    @DisplayName("Match method returns true for subscript syntax")
    void matchMethodReturnsTrueForSubscript() {
        var cur = Cursor.of("[1]", ctx.identifierQuoting());
        // The match should return true (syntax matches), but parse will fail
        assertTrue(parser.match(cur, ctx));
    }

    @Test
    @DisplayName("Target type is ArraySubscriptExpr")
    void targetTypeIsArraySubscriptExpr() {
        assertEquals(ArraySubscriptExpr.class, parser.targetType());
    }
}
