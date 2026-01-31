package io.sqm.parser.ansi;

import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CteDefParserTest {

    private final ParseContext ctx = ParseContext.of(new AnsiSpecs());
    private final CteDefParser parser = new CteDefParser();

    @Test
    @DisplayName("Rejects MATERIALIZED in ANSI")
    void rejects_materialized() {
        var res = ctx.parse(parser, "t AS MATERIALIZED (SELECT 1)");
        assertTrue(res.isError());
    }

    @Test
    @DisplayName("Rejects NOT MATERIALIZED in ANSI")
    void rejects_not_materialized() {
        var res = ctx.parse(parser, "t AS NOT MATERIALIZED (SELECT 1)");
        assertTrue(res.isError());
    }
}
