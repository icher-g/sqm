package io.sqm.parser.ansi;

import io.sqm.core.DollarStringLiteralExpr;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link DollarStringLiteralExprParser} in ANSI dialect.
 *
 * <p>Note: ANSI SQL does not support dollar-quoted strings, so all tests should fail.</p>
 */
@DisplayName("ANSI DollarStringLiteralExprParser Tests")
class DollarStringLiteralExprParserTest {

    private ParseContext ctx;
    private DollarStringLiteralExprParser parser;

    @BeforeEach
    void setUp() {
        ctx = ParseContext.of(new AnsiSpecs());
        parser = new DollarStringLiteralExprParser();
    }

    @Test
    @DisplayName("Parse dollar-quoted string is not supported in ANSI")
    void parseDollarStringNotSupported() {
        // Note: Since the tokenizer may not recognize $$ as DOLLAR_STRING token in ANSI,
        // this test verifies that the feature is not supported
        var result = ctx.parse(DollarStringLiteralExpr.class, "$$hello$$");

        assertFalse(result.ok());
        assertNotNull(result.errorMessage());
        // The error may come from tokenization or feature check
    }

    @Test
    @DisplayName("Parse dollar-quoted string with tag is not supported")
    void parseDollarStringWithTagNotSupported() {
        var result = ctx.parse(DollarStringLiteralExpr.class, "$tag$hello$tag$");

        assertFalse(result.ok());
        assertNotNull(result.errorMessage());
    }

    @Test
    @DisplayName("Parse dollar-quoted string with empty content is not supported")
    void parseDollarStringEmptyContentNotSupported() {
        var result = ctx.parse(DollarStringLiteralExpr.class, "$$$$");

        assertFalse(result.ok());
        assertNotNull(result.errorMessage());
    }

    @Test
    @DisplayName("Parse dollar-quoted string with newlines is not supported")
    void parseDollarStringWithNewlinesNotSupported() {
        var result = ctx.parse(DollarStringLiteralExpr.class, "$$hello\nworld$$");

        assertFalse(result.ok());
        assertNotNull(result.errorMessage());
    }

    @Test
    @DisplayName("Target type is DollarStringLiteralExpr")
    void targetTypeIsDollarStringLiteralExpr() {
        assertEquals(DollarStringLiteralExpr.class, parser.targetType());
    }
}
