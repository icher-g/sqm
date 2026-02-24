package io.sqm.parser.ansi;

import io.sqm.core.QuoteStyle;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    @DisplayName("Preserves quote style for CTE name and column aliases")
    void preserves_quote_style_for_cte_identifiers() {
        var res = ctx.parse(parser, "\"MyCte\"(\"col1\", \"col2\") AS (SELECT 1)");
        assertTrue(res.ok(), () -> String.valueOf(res.errorMessage()));

        var cte = res.value();
        assertEquals("MyCte", cte.name().value());
        assertEquals(QuoteStyle.DOUBLE_QUOTE, cte.name().quoteStyle());
        assertEquals(2, cte.columnAliases().size());
        assertEquals(QuoteStyle.DOUBLE_QUOTE, cte.columnAliases().get(0).quoteStyle());
        assertEquals(QuoteStyle.DOUBLE_QUOTE, cte.columnAliases().get(1).quoteStyle());
    }
}
