package io.sqm.parser.ansi;

import io.sqm.core.ArraySubscriptExpr;
import io.sqm.core.Expression;
import io.sqm.parser.AtomicExprParser;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ArraySubscriptExprParser}.
 *
 * <p>Tests both feature rejection (ANSI) and actual parsing logic (TestSpecs).</p>
 */
@DisplayName("ArraySubscriptExprParser Tests")
class ArraySubscriptExprParserTest {

    private ParseContext ansiCtx;
    private ParseContext testCtx;
    private ArraySubscriptExprParser parser;

    @BeforeEach
    void setUp() {
        ansiCtx = ParseContext.of(new AnsiSpecs());
        testCtx = ParseContext.of(new TestSpecs());
        parser = new ArraySubscriptExprParser(new AtomicExprParser());
    }

    @Test
    @DisplayName("Parse array subscript is not supported in ANSI")
    void parseArraySubscriptNotSupported() {
        var result = ansiCtx.parse(ArraySubscriptExpr.class, "arr[1]");

        assertFalse(result.ok());
        assertNotNull(result.errorMessage());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Array subscripts are not supported"));
    }

    @Test
    @DisplayName("Parse array subscript with literal index is not supported")
    void parseArraySubscriptWithLiteralIndexNotSupported() {
        var result = ansiCtx.parse(ArraySubscriptExpr.class, "col[5]");

        assertFalse(result.ok());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Array subscripts are not supported"));
    }

    @Test
    @DisplayName("Parse array subscript with literal index")
    void parseArraySubscriptWithLiteralIndex() {
        var result = testCtx.parse(ArraySubscriptExpr.class, "arr[1]");

        assertTrue(result.ok());
        var expr = result.value();
        assertNotNull(expr);
        assertNotNull(expr.base());
        assertNotNull(expr.index());
    }

    @Test
    @DisplayName("Parse array subscript with column index")
    void parseArraySubscriptWithColumnIndex() {
        var result = testCtx.parse(ArraySubscriptExpr.class, "data[idx]");

        assertTrue(result.ok());
        var expr = result.value();
        assertNotNull(expr);
        assertNotNull(expr.base());
        assertNotNull(expr.index());
    }

    @Test
    @DisplayName("Parse nested array subscript")
    void parseNestedArraySubscript() {
        var result = testCtx.parse(Expression.class, "matrix[1][2]");

        assertTrue(result.ok());
        var expr = (ArraySubscriptExpr)result.value();
        assertNotNull(expr);
        // Outer subscript
        assertNotNull(expr.base());
        assertNotNull(expr.index());
        // Inner subscript is the base
        assertInstanceOf(ArraySubscriptExpr.class, expr.base());
    }

    @Test
    @DisplayName("Parse array subscript with column index is not supported")
    void parseArraySubscriptWithColumnIndexNotSupported() {
        var result = ansiCtx.parse(ArraySubscriptExpr.class, "arr[idx]");

        assertFalse(result.ok());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Array subscripts are not supported"));
    }

    @Test
    @DisplayName("Parse nested array subscript is not supported")
    void parseNestedArraySubscriptNotSupported() {
        var result = ansiCtx.parse(ArraySubscriptExpr.class, "arr[1][2]");

        assertFalse(result.ok());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Array subscripts are not supported"));
    }

    @Test
    @DisplayName("Match method returns false when colon present (slice)")
    void matchMethodReturnsFalseForSlice() {
        var cur = Cursor.of("[1:3]", ansiCtx.identifierQuoting());
        // Should match as false since this is a slice, not subscript
        assertFalse(parser.match(cur, ansiCtx));
    }

    @Test
    @DisplayName("Match method returns true for subscript syntax")
    void matchMethodReturnsTrueForSubscript() {
        var cur = Cursor.of("[1]", ansiCtx.identifierQuoting());
        // The match should return true (syntax matches), but parse will fail
        assertTrue(parser.match(cur, ansiCtx));
    }

    @Test
    @DisplayName("Target type is ArraySubscriptExpr")
    void targetTypeIsArraySubscriptExpr() {
        assertEquals(ArraySubscriptExpr.class, parser.targetType());
    }
}
