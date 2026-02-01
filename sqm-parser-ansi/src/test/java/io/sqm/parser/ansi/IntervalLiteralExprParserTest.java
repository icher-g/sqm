package io.sqm.parser.ansi;

import io.sqm.core.IntervalLiteralExpr;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link IntervalLiteralExprParser}.
 */
@DisplayName("ANSI IntervalLiteralExprParser Tests")
class IntervalLiteralExprParserTest {

    private ParseContext ctx;
    private IntervalLiteralExprParser parser;

    @BeforeEach
    void setUp() {
        ctx = ParseContext.of(new AnsiSpecs());
        parser = new IntervalLiteralExprParser();
    }

    @Test
    @DisplayName("Parse simple INTERVAL literal without qualifier")
    void parseSimpleIntervalWithoutQualifier() {
        var result = ctx.parse(IntervalLiteralExpr.class, "INTERVAL '1 day'");

        assertTrue(result.ok());
        var interval = result.value();
        assertNotNull(interval);
        assertEquals("1 day", interval.value());
        assertTrue(interval.qualifier().isEmpty());
    }

    @Test
    @DisplayName("Parse INTERVAL with DAY qualifier")
    void parseIntervalWithDayQualifier() {
        var result = ctx.parse(IntervalLiteralExpr.class, "INTERVAL '5' DAY");

        assertTrue(result.ok());
        var interval = result.value();
        assertNotNull(interval);
        assertEquals("5", interval.value());
        assertTrue(interval.qualifier().isPresent());
        assertEquals("DAY", interval.qualifier().get());
    }

    @Test
    @DisplayName("Parse INTERVAL with YEAR qualifier")
    void parseIntervalWithYearQualifier() {
        var result = ctx.parse(IntervalLiteralExpr.class, "INTERVAL '2' YEAR");

        assertTrue(result.ok());
        var interval = result.value();
        assertNotNull(interval);
        assertEquals("2", interval.value());
        assertTrue(interval.qualifier().isPresent());
        assertEquals("YEAR", interval.qualifier().get());
    }

    @Test
    @DisplayName("Parse INTERVAL with MONTH qualifier")
    void parseIntervalWithMonthQualifier() {
        var result = ctx.parse(IntervalLiteralExpr.class, "INTERVAL '3' MONTH");

        assertTrue(result.ok());
        var interval = result.value();
        assertNotNull(interval);
        assertTrue(interval.qualifier().isPresent());
        assertEquals("MONTH", interval.qualifier().get());
    }

    @Test
    @DisplayName("Parse INTERVAL with HOUR qualifier")
    void parseIntervalWithHourQualifier() {
        var result = ctx.parse(IntervalLiteralExpr.class, "INTERVAL '12' HOUR");

        assertTrue(result.ok());
        var interval = result.value();
        assertNotNull(interval);
        assertTrue(interval.qualifier().isPresent());
        assertEquals("HOUR", interval.qualifier().get());
    }

    @Test
    @DisplayName("Parse INTERVAL with MINUTE qualifier")
    void parseIntervalWithMinuteQualifier() {
        var result = ctx.parse(IntervalLiteralExpr.class, "INTERVAL '30' MINUTE");

        assertTrue(result.ok());
        var interval = result.value();
        assertNotNull(interval);
        assertTrue(interval.qualifier().isPresent());
        assertEquals("MINUTE", interval.qualifier().get());
    }

    @Test
    @DisplayName("Parse INTERVAL with SECOND qualifier")
    void parseIntervalWithSecondQualifier() {
        var result = ctx.parse(IntervalLiteralExpr.class, "INTERVAL '45' SECOND");

        assertTrue(result.ok());
        var interval = result.value();
        assertNotNull(interval);
        assertTrue(interval.qualifier().isPresent());
        assertEquals("SECOND", interval.qualifier().get());
    }

    @Test
    @DisplayName("Parse INTERVAL with YEAR TO MONTH qualifier")
    void parseIntervalWithYearToMonthQualifier() {
        var result = ctx.parse(IntervalLiteralExpr.class, "INTERVAL '2-3' YEAR TO MONTH");

        assertTrue(result.ok());
        var interval = result.value();
        assertNotNull(interval);
        assertEquals("2-3", interval.value());
        assertTrue(interval.qualifier().isPresent());
        assertEquals("YEAR TO MONTH", interval.qualifier().get());
    }

    @Test
    @DisplayName("Parse INTERVAL with DAY TO HOUR qualifier")
    void parseIntervalWithDayToHourQualifier() {
        var result = ctx.parse(IntervalLiteralExpr.class, "INTERVAL '1 12' DAY TO HOUR");

        assertTrue(result.ok());
        var interval = result.value();
        assertNotNull(interval);
        assertTrue(interval.qualifier().isPresent());
        assertEquals("DAY TO HOUR", interval.qualifier().get());
    }

    @Test
    @DisplayName("Parse INTERVAL with DAY TO MINUTE qualifier")
    void parseIntervalWithDayToMinuteQualifier() {
        var result = ctx.parse(IntervalLiteralExpr.class, "INTERVAL '1 12:30' DAY TO MINUTE");

        assertTrue(result.ok());
        var interval = result.value();
        assertNotNull(interval);
        assertTrue(interval.qualifier().isPresent());
        assertEquals("DAY TO MINUTE", interval.qualifier().get());
    }

    @Test
    @DisplayName("Parse INTERVAL with DAY TO SECOND qualifier")
    void parseIntervalWithDayToSecondQualifier() {
        var result = ctx.parse(IntervalLiteralExpr.class, "INTERVAL '1 12:30:45' DAY TO SECOND");

        assertTrue(result.ok());
        var interval = result.value();
        assertNotNull(interval);
        assertTrue(interval.qualifier().isPresent());
        assertEquals("DAY TO SECOND", interval.qualifier().get());
    }

    @Test
    @DisplayName("Parse INTERVAL with HOUR TO MINUTE qualifier")
    void parseIntervalWithHourToMinuteQualifier() {
        var result = ctx.parse(IntervalLiteralExpr.class, "INTERVAL '12:30' HOUR TO MINUTE");

        assertTrue(result.ok());
        var interval = result.value();
        assertNotNull(interval);
        assertTrue(interval.qualifier().isPresent());
        assertEquals("HOUR TO MINUTE", interval.qualifier().get());
    }

    @Test
    @DisplayName("Parse INTERVAL with HOUR TO SECOND qualifier")
    void parseIntervalWithHourToSecondQualifier() {
        var result = ctx.parse(IntervalLiteralExpr.class, "INTERVAL '12:30:45' HOUR TO SECOND");

        assertTrue(result.ok());
        var interval = result.value();
        assertNotNull(interval);
        assertTrue(interval.qualifier().isPresent());
        assertEquals("HOUR TO SECOND", interval.qualifier().get());
    }

    @Test
    @DisplayName("Parse INTERVAL with MINUTE TO SECOND qualifier")
    void parseIntervalWithMinuteToSecondQualifier() {
        var result = ctx.parse(IntervalLiteralExpr.class, "INTERVAL '30:45' MINUTE TO SECOND");

        assertTrue(result.ok());
        var interval = result.value();
        assertNotNull(interval);
        assertTrue(interval.qualifier().isPresent());
        assertEquals("MINUTE TO SECOND", interval.qualifier().get());
    }

    @Test
    @DisplayName("Parse INTERVAL case insensitive")
    void parseIntervalCaseInsensitive() {
        var result1 = ctx.parse(IntervalLiteralExpr.class, "interval '1' day");
        var result2 = ctx.parse(IntervalLiteralExpr.class, "INTERVAL '1' DAY");
        var result3 = ctx.parse(IntervalLiteralExpr.class, "Interval '1' Day");

        assertTrue(result1.ok());
        assertTrue(result2.ok());
        assertTrue(result3.ok());
    }

    @Test
    @DisplayName("Parse without INTERVAL keyword fails")
    void parseWithoutIntervalKeywordFails() {
        var result = ctx.parse(IntervalLiteralExpr.class, "'1 day'");

        assertFalse(result.ok());
        assertNotNull(result.errorMessage());
    }

    @Test
    @DisplayName("Parse INTERVAL without string literal fails")
    void parseWithoutStringLiteralFails() {
        var result = ctx.parse(IntervalLiteralExpr.class, "INTERVAL 1");

        assertFalse(result.ok());
        assertNotNull(result.errorMessage());
    }

    @Test
    @DisplayName("Match returns true for valid INTERVAL literal")
    void matchReturnsTrueForValidInterval() {
        var cur = Cursor.of("INTERVAL '1 day'", ctx.identifierQuoting());
        assertTrue(parser.match(cur, ctx));
    }

    @Test
    @DisplayName("Match returns false for non-INTERVAL keyword")
    void matchReturnsFalseForNonInterval() {
        var cur = Cursor.of("SELECT '1 day'", ctx.identifierQuoting());
        assertFalse(parser.match(cur, ctx));
    }

    @Test
    @DisplayName("Match returns false when string literal missing")
    void matchReturnsFalseWhenStringMissing() {
        var cur = Cursor.of("INTERVAL", ctx.identifierQuoting());
        assertFalse(parser.match(cur, ctx));
    }

    @Test
    @DisplayName("Target type is IntervalLiteralExpr")
    void targetTypeIsIntervalLiteralExpr() {
        assertEquals(IntervalLiteralExpr.class, parser.targetType());
    }

    @Test
    @DisplayName("Parse INTERVAL with invalid unit throws error")
    void parseWithInvalidUnitThrowsError() {
        var result = ctx.parse(IntervalLiteralExpr.class, "INTERVAL '1' INVALID TO UNIT");

        assertFalse(result.ok());
        assertNotNull(result.errorMessage());
    }
}
