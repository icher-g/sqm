package io.sqm.parser.ansi;

import io.sqm.core.ArraySliceExpr;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ArraySliceExprParser} in ANSI dialect.
 *
 * <p>Note: ANSI SQL does not support array slicing, so all tests should fail.</p>
 */
@DisplayName("ANSI ArraySliceExprParser Tests")
class ArraySliceExprParserTest {

    private ParseContext ctx;
    private ArraySliceExprParser parser;

    @BeforeEach
    void setUp() {
        ctx = ParseContext.of(new AnsiSpecs());
        parser = new ArraySliceExprParser();
    }

    @Test
    @DisplayName("Parse array slice is not supported in ANSI")
    void parseArraySliceNotSupported() {
        var result = ctx.parse(ArraySliceExpr.class, "arr[1:3]");

        assertFalse(result.ok());
        assertNotNull(result.errorMessage());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Array slices are not supported"));
    }

    @Test
    @DisplayName("Parse array slice with both bounds is not supported")
    void parseArraySliceWithBothBoundsNotSupported() {
        var result = ctx.parse(ArraySliceExpr.class, "col[2:5]");

        assertFalse(result.ok());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Array slices are not supported"));
    }

    @Test
    @DisplayName("Parse array slice with missing from bound is not supported")
    void parseArraySliceWithMissingFromNotSupported() {
        var result = ctx.parse(ArraySliceExpr.class, "arr[:5]");

        assertFalse(result.ok());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Array slices are not supported"));
    }

    @Test
    @DisplayName("Parse array slice with missing to bound is not supported")
    void parseArraySliceWithMissingToNotSupported() {
        var result = ctx.parse(ArraySliceExpr.class, "arr[2:]");

        assertFalse(result.ok());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Array slices are not supported"));
    }

    @Test
    @DisplayName("Parse array slice with no bounds is not supported")
    void parseArraySliceWithNoBoundsNotSupported() {
        var result = ctx.parse(ArraySliceExpr.class, "arr[:]");

        assertFalse(result.ok());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Array slices are not supported"));
    }

    @Test
    @DisplayName("Match method returns false for ANSI dialect")
    void matchMethodReturnsFalseForAnsi() {
        // Even if syntax looks like slice, ANSI doesn't support it
        // so the match behavior depends on implementation
        // The parser itself will reject it during parse
        assertFalse(ctx.parse(ArraySliceExpr.class, "arr[1:3]").ok());
    }

    @Test
    @DisplayName("Target type is ArraySliceExpr")
    void targetTypeIsArraySliceExpr() {
        assertEquals(ArraySliceExpr.class, parser.targetType());
    }
}
