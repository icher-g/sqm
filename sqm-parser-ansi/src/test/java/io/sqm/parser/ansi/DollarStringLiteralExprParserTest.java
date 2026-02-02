package io.sqm.parser.ansi;

import io.sqm.core.DollarStringLiteralExpr;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link DollarStringLiteralExprParser}.
 *
 * <p>Tests both feature rejection (ANSI) and actual parsing logic (TestSpecs).</p>
 */
@DisplayName("DollarStringLiteralExprParser Tests")
class DollarStringLiteralExprParserTest {

    private ParseContext ansiCtx;
    private ParseContext testCtx;
    private DollarStringLiteralExprParser parser;

    @BeforeEach
    void setUp() {
        ansiCtx = ParseContext.of(new AnsiSpecs());
        testCtx = ParseContext.of(new TestSpecs());
        parser = new DollarStringLiteralExprParser();
    }

    @Test
    @DisplayName("Parse dollar-quoted string is not supported in ANSI")
    void parseDollarStringNotSupported() {
        // Note: Since the tokenizer may not recognize $$ as DOLLAR_STRING token in ANSI,
        // this test verifies that the feature is not supported
        var result = ansiCtx.parse(DollarStringLiteralExpr.class, "$$hello$$");

        assertFalse(result.ok());
        assertNotNull(result.errorMessage());
        // The error may come from tokenization or feature check
    }

    @Test
    @DisplayName("Parse dollar-quoted string with tag is not supported")
    void parseDollarStringWithTagNotSupported() {
        var result = ansiCtx.parse(DollarStringLiteralExpr.class, "$tag$hello$tag$");

        assertFalse(result.ok());
        assertNotNull(result.errorMessage());
    }

    @Test
    @DisplayName("Parse dollar-quoted string with empty content is not supported")
    void parseDollarStringEmptyContentNotSupported() {
        var result = ansiCtx.parse(DollarStringLiteralExpr.class, "$$$$");

        assertFalse(result.ok());
        assertNotNull(result.errorMessage());
    }

    @Test
    @DisplayName("Parse dollar-quoted string with newlines is not supported")
    void parseDollarStringWithNewlinesNotSupported() {
        var result = ansiCtx.parse(DollarStringLiteralExpr.class, "$$hello\nworld$$");

        assertFalse(result.ok());
        assertNotNull(result.errorMessage());
    }

    @Test
    @DisplayName("Parse simple dollar-quoted string")
    void parseSimpleDollarString() {
        var result = testCtx.parse(DollarStringLiteralExpr.class, "$$hello$$");

        assertTrue(result.ok());
        var expr = result.value();
        assertNotNull(expr);
        assertEquals("hello", expr.value());
        assertTrue(expr.tag() == null || expr.tag().isEmpty());
    }

    @Test
    @DisplayName("Parse dollar-quoted string with tag")
    void parseDollarStringWithTag() {
        var result = testCtx.parse(DollarStringLiteralExpr.class, "$tag$hello world$tag$");

        assertTrue(result.ok());
        var expr = result.value();
        assertNotNull(expr);
        assertEquals("hello world", expr.value());
        assertEquals("tag", expr.tag());
    }

    @Test
    @DisplayName("Parse dollar-quoted string with empty content")
    void parseDollarStringEmptyContent() {
        var result = testCtx.parse(DollarStringLiteralExpr.class, "$$$$");

        assertTrue(result.ok());
        var expr = result.value();
        assertNotNull(expr);
        assertEquals("", expr.value());
        assertTrue(expr.tag() == null || expr.tag().isEmpty());
    }

    @Test
    @DisplayName("Parse dollar-quoted string with newlines and special characters")
    void parseDollarStringWithSpecialChars() {
        var result = testCtx.parse(DollarStringLiteralExpr.class, "$$line1\nline2\t'quotes'$$");

        assertTrue(result.ok());
        var expr = result.value();
        assertNotNull(expr);
        assertEquals("line1\nline2\t'quotes'", expr.value());
    }

    @Test
    @DisplayName("Parse dollar-quoted string with nested dollar signs")
    void parseDollarStringWithNestedDollars() {
        var result = testCtx.parse(DollarStringLiteralExpr.class, "$outer$$inner$text$inner$$outer$");

        assertTrue(result.ok());
        var expr = result.value();
        assertNotNull(expr);
        assertEquals("$inner$text$inner$", expr.value());
        assertEquals("outer", expr.tag());
    }

    @Test
    @DisplayName("Target type is DollarStringLiteralExpr")
    void targetTypeIsDollarStringLiteralExpr() {
        assertEquals(DollarStringLiteralExpr.class, parser.targetType());
    }
}
