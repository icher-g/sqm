package io.sqm.parser.ansi;

import io.sqm.core.LiteralExpr;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LiteralExprParserTest {

    private final ParseContext ctx = ParseContext.of(new AnsiSpecs());
    private final LiteralExprParser parser = new LiteralExprParser();

    @Test
    void parses_basic_literals() {
        assertEquals("text", parse(" 'text' ").value());
        assertEquals(Boolean.TRUE, parse("TRUE").value());
        assertEquals(Boolean.FALSE, parse("FALSE").value());
        assertNull(parse("NULL").value());
    }

    @Test
    void parses_numbers_as_long_or_double() {
        assertEquals(10L, parse("10").value());
        assertEquals(3.14d, parse("3.14").value());
        assertEquals(1.0e3d, parse("1e3").value());
    }

    @Test
    void rejects_invalid_typed_literal_prefixes() {
        var result = parser.parse(Cursor.of("DATE", ctx.identifierQuoting()), ctx);
        assertTrue(result.isError());

        var invalid = parser.parse(Cursor.of("foo", ctx.identifierQuoting()), ctx);
        assertTrue(invalid.isError());
    }

    private LiteralExpr parse(String spec) {
        var result = parser.parse(Cursor.of(spec, ctx.identifierQuoting()), ctx);
        assertTrue(result.ok(), () -> result.errorMessage());
        return result.value();
    }
}
