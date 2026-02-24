package io.sqm.parser.ansi;

import io.sqm.core.*;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.tbl;
import static org.junit.jupiter.api.Assertions.*;

class JoinClauseParsersTest {

    private final ParseContext ctx = ParseContext.of(new AnsiSpecs());

    @Test
    void parsesOnJoinWithKind() {
        var parser = new OnJoinParser();

        var result = ctx.parse(parser, "LEFT JOIN t ON a = b");

        assertTrue(result.ok());
        assertEquals(JoinKind.LEFT, result.value().kind());
    }

    @Test
    void parsesOnJoinInfix() {
        var parser = new OnJoinParser();
        var cur = Cursor.of("ON a = b", ctx.identifierQuoting());

        var result = parser.parse(tbl("t"), cur, ctx);

        assertTrue(result.ok());
        assertInstanceOf(OnJoin.class, result.value());
    }

    @Test
    void parsesUsingJoinWithColumns() {
        var parser = new UsingJoinParser();

        var result = ctx.parse(parser, "JOIN t USING (a, b)");

        assertTrue(result.ok());
        assertEquals(JoinKind.INNER, result.value().kind());
        assertEquals(2, result.value().usingColumns().size());
    }

    @Test
    void parsesUsingJoinInfix() {
        var parser = new UsingJoinParser();
        var cur = Cursor.of("USING (a)", ctx.identifierQuoting());

        var result = parser.parse(TableRef.table(io.sqm.core.Identifier.of("t")), cur, ctx);

        assertTrue(result.ok());
        assertInstanceOf(UsingJoin.class, result.value());
        assertEquals(1, result.value().usingColumns().size());
    }

    @Test
    void preserves_quote_metadata_in_using_columns() {
        var parser = new UsingJoinParser();
        var cur = Cursor.of("USING (\"A\")", ctx.identifierQuoting());

        var result = parser.parse(tbl("t"), cur, ctx);

        assertTrue(result.ok());
        assertEquals("A", result.value().usingColumns().getFirst().value());
        assertEquals(QuoteStyle.DOUBLE_QUOTE, result.value().usingColumns().getFirst().quoteStyle());
    }
}

