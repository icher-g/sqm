package io.sqm.parser.ansi;

import io.sqm.core.ColumnExpr;
import io.sqm.core.QuoteStyle;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ColumnExprParserQuoteMetadataTest {
    private final ParseContext ctx = ParseContext.of(new AnsiSpecs());

    @Test
    void preserves_quote_metadata_for_qualified_column() {
        var r = ctx.parse(ColumnExpr.class, "\"U\".\"Name\"");
        assertTrue(r.ok(), () -> "problems: " + r.problems());
        var col = r.value();
        assertEquals("U", col.tableAlias().value());
        assertEquals("Name", col.name().value());
        assertEquals(QuoteStyle.DOUBLE_QUOTE, col.tableAlias().quoteStyle());
        assertEquals(QuoteStyle.DOUBLE_QUOTE, col.name().quoteStyle());
    }

    @Test
    void leaves_unquoted_column_unquoted() {
        var r = ctx.parse(ColumnExpr.class, "name");
        assertTrue(r.ok(), () -> "problems: " + r.problems());
        var col = r.value();
        assertNull(col.tableAlias());
        assertEquals(QuoteStyle.NONE, col.name().quoteStyle());
    }
}
