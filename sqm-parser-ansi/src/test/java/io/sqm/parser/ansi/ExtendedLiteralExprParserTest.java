package io.sqm.parser.ansi;

import io.sqm.core.*;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExtendedLiteralExprParserTest {

    private ParseContext ctx;

    @BeforeEach
    void setUp() {
        ctx = ParseContext.of(new AnsiSpecs());
    }

    @Test
    void parses_date_literal() {
        var result = ctx.parse(Expression.class, "DATE '2020-01-01'");
        assertTrue(result.ok());
        var expr = assertInstanceOf(DateLiteralExpr.class, result.value());
        assertEquals("2020-01-01", expr.value());
    }

    @Test
    void parses_time_literal_with_time_zone() {
        var result = ctx.parse(Expression.class, "TIME WITH TIME ZONE '10:11:12'");
        assertTrue(result.ok());
        var expr = assertInstanceOf(TimeLiteralExpr.class, result.value());
        assertEquals(TimeZoneSpec.WITH_TIME_ZONE, expr.timeZoneSpec());
        assertEquals("10:11:12", expr.value());
    }

    @Test
    void parses_time_literal_without_time_zone() {
        var result = ctx.parse(Expression.class, "TIME '10:11:12'");
        assertTrue(result.ok());
        var expr = assertInstanceOf(TimeLiteralExpr.class, result.value());
        assertEquals(TimeZoneSpec.NONE, expr.timeZoneSpec());
        assertEquals("10:11:12", expr.value());
    }

    @Test
    void parses_timestamp_literal_without_time_zone() {
        var result = ctx.parse(Expression.class, "TIMESTAMP WITHOUT TIME ZONE '2020-01-01 00:00:00'");
        assertTrue(result.ok());
        var expr = assertInstanceOf(TimestampLiteralExpr.class, result.value());
        assertEquals(TimeZoneSpec.WITHOUT_TIME_ZONE, expr.timeZoneSpec());
        assertEquals("2020-01-01 00:00:00", expr.value());
    }

    @Test
    void rejects_invalid_time_zone_clause() {
        var result = ctx.parse(Expression.class, "TIME WITH ZONE '10:11:12'");
        assertTrue(result.isError());
    }

    @Test
    void parses_bit_and_hex_literals() {
        var bit = ctx.parse(Expression.class, "B'1010'");
        assertTrue(bit.ok());
        assertEquals("1010", assertInstanceOf(BitStringLiteralExpr.class, bit.value()).value());

        var hex = ctx.parse(Expression.class, "X'FF'");
        assertTrue(hex.ok());
        assertEquals("FF", assertInstanceOf(HexStringLiteralExpr.class, hex.value()).value());
    }

    @Test
    void rejects_escape_string_literal() {
        var result = ctx.parse(Expression.class, "E'it\\'s'");
        assertTrue(result.isError());
    }

    @Test
    void rejects_dollar_quoted_literal() {
        var result = ctx.parse(Expression.class, "$$hello$$");
        assertTrue(result.isError());
    }

    @Test
    void parses_interval_literal() {
        var result = ctx.parse(Expression.class, "INTERVAL '1 day'");
        assertTrue(result.ok());
        var expr = assertInstanceOf(IntervalLiteralExpr.class, result.value());
        assertEquals("1 day", expr.value());
        assertTrue(expr.qualifier().isEmpty());
    }
}
