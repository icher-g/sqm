package io.sqm.parser.ansi;

import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TypedLiteralExprParserMatchTest {

    private final ParseContext ctx = ParseContext.of(new AnsiSpecs());

    @Test
    void date_parser_match_requires_string_literal() {
        var parser = new DateLiteralExprParser();
        assertTrue(parser.match(Cursor.of("DATE '2020-01-01'", ctx.identifierQuoting()), ctx));
        assertFalse(parser.match(Cursor.of("DATE", ctx.identifierQuoting()), ctx));
        assertFalse(parser.match(Cursor.of("TIME '10:11:12'", ctx.identifierQuoting()), ctx));
    }

    @Test
    void time_parser_match_handles_time_zone_clause() {
        var parser = new TimeLiteralExprParser();
        assertTrue(parser.match(Cursor.of("TIME '10:11:12'", ctx.identifierQuoting()), ctx));
        assertTrue(parser.match(Cursor.of("TIME WITH TIME ZONE '10:11:12'", ctx.identifierQuoting()), ctx));
        assertTrue(parser.match(Cursor.of("TIME WITHOUT TIME ZONE '10:11:12'", ctx.identifierQuoting()), ctx));
        assertFalse(parser.match(Cursor.of("TIME WITH ZONE '10:11:12'", ctx.identifierQuoting()), ctx));
    }

    @Test
    void timestamp_parser_match_handles_time_zone_clause() {
        var parser = new TimestampLiteralExprParser();
        assertTrue(parser.match(Cursor.of("TIMESTAMP '2020-01-01 00:00:00'", ctx.identifierQuoting()), ctx));
        assertTrue(parser.match(Cursor.of("TIMESTAMP WITH TIME ZONE '2020-01-01 00:00:00'", ctx.identifierQuoting()), ctx));
        assertTrue(parser.match(Cursor.of("TIMESTAMP WITHOUT TIME ZONE '2020-01-01 00:00:00'", ctx.identifierQuoting()), ctx));
        assertFalse(parser.match(Cursor.of("TIMESTAMP WITH ZONE '2020-01-01 00:00:00'", ctx.identifierQuoting()), ctx));
    }
}
