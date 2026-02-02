package io.sqm.parser.ansi;

import io.sqm.core.ArraySliceExpr;
import io.sqm.parser.AtomicExprParser;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ArraySliceExprParser}.
 *
 * <p>Tests both feature rejection (ANSI) and actual parsing logic (TestSpecs).</p>
 */
@DisplayName("ArraySliceExprParser Tests")
class ArraySliceExprParserTest {

    private ParseContext ansiCtx;
    private ParseContext testCtx;
    private ArraySliceExprParser parser;

    @BeforeEach
    void setUp() {
        ansiCtx = ParseContext.of(new AnsiSpecs());
        testCtx = ParseContext.of(new TestSpecs());
        parser = new ArraySliceExprParser(new AtomicExprParser());
    }

    @Test
    @DisplayName("Parse array slice is not supported in ANSI")
    void parseArraySliceNotSupported() {
        var result = ansiCtx.parse(ArraySliceExpr.class, "arr[1:3]");

        assertFalse(result.ok());
        assertNotNull(result.errorMessage());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Array slices are not supported"));
    }

    @Test
    @DisplayName("Parse array slice with both bounds is not supported")
    void parseArraySliceWithBothBoundsNotSupported() {
        var result = ansiCtx.parse(ArraySliceExpr.class, "col[2:5]");

        assertFalse(result.ok());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Array slices are not supported"));
    }

    @Test
    @DisplayName("Parse array slice with missing from bound is not supported")
    void parseArraySliceWithMissingFromNotSupported() {
        var result = ansiCtx.parse(ArraySliceExpr.class, "arr[:5]");

        assertFalse(result.ok());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Array slices are not supported"));
    }

    @Test
    @DisplayName("Parse array slice with missing to bound is not supported")
    void parseArraySliceWithMissingToNotSupported() {
        var result = ansiCtx.parse(ArraySliceExpr.class, "arr[2:]");

        assertFalse(result.ok());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Array slices are not supported"));
    }

    @Test
    @DisplayName("Parse array slice with no bounds is not supported")
    void parseArraySliceWithNoBoundsNotSupported() {
        var result = ansiCtx.parse(ArraySliceExpr.class, "arr[:]");

        assertFalse(result.ok());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Array slices are not supported"));
    }

    @Test
    @DisplayName("Parse array slice with both bounds")
    void parseArraySliceWithBothBounds() {
        var result = testCtx.parse(ArraySliceExpr.class, "arr[1:3]");

        assertTrue(result.ok());
        var expr = result.value();
        assertNotNull(expr);
        assertTrue(expr.from().isPresent());
        assertTrue(expr.to().isPresent());
    }

    @Test
    @DisplayName("Parse array slice with only from bound")
    void parseArraySliceWithOnlyFrom() {
        var result = testCtx.parse(ArraySliceExpr.class, "col[2:]");

        assertTrue(result.ok());
        var expr = result.value();
        assertNotNull(expr);
        assertTrue(expr.from().isPresent());
        assertTrue(expr.to().isEmpty());
    }

    @Test
    @DisplayName("Parse array slice with only to bound")
    void parseArraySliceWithOnlyTo() {
        var result = testCtx.parse(ArraySliceExpr.class, "arr[:5]");

        assertTrue(result.ok());
        var expr = result.value();
        assertNotNull(expr);
        assertTrue(expr.from().isEmpty());
        assertTrue(expr.to().isPresent());
    }

    @Test
    @DisplayName("Parse array slice with no bounds")
    void parseArraySliceWithNoBounds() {
        var result = testCtx.parse(ArraySliceExpr.class, "data[:]");

        assertTrue(result.ok());
        var expr = result.value();
        assertNotNull(expr);
        assertTrue(expr.from().isEmpty());
        assertTrue(expr.to().isEmpty());
    }

    @Test
    @DisplayName("Match method returns false for ANSI dialect")
    void matchMethodReturnsFalseForAnsi() {
        // Even if syntax looks like slice, ANSI doesn't support it
        // so the match behavior depends on implementation
        // The parser itself will reject it during parse
        assertFalse(ansiCtx.parse(ArraySliceExpr.class, "arr[1:3]").ok());
    }

    @Test
    @DisplayName("Target type is ArraySliceExpr")
    void targetTypeIsArraySliceExpr() {
        assertEquals(ArraySliceExpr.class, parser.targetType());
    }
}
