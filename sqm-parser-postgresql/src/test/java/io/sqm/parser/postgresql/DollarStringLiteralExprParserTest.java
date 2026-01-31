package io.sqm.parser.postgresql;

import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.Token;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DollarStringLiteralExprParserTest {

    private final ParseContext ctx = ParseContext.of(new io.sqm.parser.postgresql.spi.PostgresSpecs());
    private final DollarStringLiteralExprParser parser = new DollarStringLiteralExprParser();

    @Test
    void parses_empty_tag_literal() {
        var cur = Cursor.of("$$hello$$", ctx.identifierQuoting());
        var result = parser.parse(cur, ctx);
        assertTrue(result.ok());
        assertEquals("", result.value().tag());
        assertEquals("hello", result.value().value());
    }

    @Test
    void rejects_invalid_dollar_literal_token() {
        var tokens = List.of(
            new Token(TokenType.DOLLAR_STRING, "$tag$bad", 0),
            new Token(TokenType.EOF, "", 8)
        );
        var cur = new Cursor(tokens);
        var result = parser.parse(cur, ctx);
        assertTrue(result.isError());
    }
}
