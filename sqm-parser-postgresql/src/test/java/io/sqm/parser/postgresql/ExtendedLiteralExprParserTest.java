package io.sqm.parser.postgresql;

import io.sqm.core.*;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExtendedLiteralExprParserTest {

    private ParseContext ctx;

    @BeforeEach
    void setUp() {
        ctx = ParseContext.of(new io.sqm.parser.postgresql.spi.PostgresSpecs());
    }

    @Test
    void parses_escape_string_literal() {
        var result = ctx.parse(Expression.class, "E'it\\'s'");
        assertTrue(result.ok());
        var expr = assertInstanceOf(EscapeStringLiteralExpr.class, result.value());
        assertEquals("it\\'s", expr.value());
    }

    @Test
    void parses_dollar_quoted_literal() {
        var result = ctx.parse(Expression.class, "$$hello$$");
        assertTrue(result.ok());
        var expr = assertInstanceOf(DollarStringLiteralExpr.class, result.value());
        assertEquals("", expr.tag());
        assertEquals("hello", expr.value());
    }

    @Test
    void parses_tagged_dollar_quoted_literal() {
        var result = ctx.parse(Expression.class, "$tag$world$tag$");
        assertTrue(result.ok());
        var expr = assertInstanceOf(DollarStringLiteralExpr.class, result.value());
        assertEquals("tag", expr.tag());
        assertEquals("world", expr.value());
    }

    @Test
    void parses_interval_literal_with_qualifier() {
        var result = ctx.parse(Expression.class, "INTERVAL '1' DAY");
        assertTrue(result.ok());
        var expr = assertInstanceOf(IntervalLiteralExpr.class, result.value());
        assertEquals("1", expr.value());
        assertEquals("DAY", expr.qualifier().orElse(null));
    }

    @Test
    void parses_interval_literal_without_qualifier() {
        var result = ctx.parse(Expression.class, "INTERVAL '1 day'");
        assertTrue(result.ok());
        var expr = assertInstanceOf(IntervalLiteralExpr.class, result.value());
        assertEquals("1 day", expr.value());
        assertTrue(expr.qualifier().isEmpty());
    }
}
