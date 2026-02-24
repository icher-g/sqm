package io.sqm.parser.ansi;

import io.sqm.core.QualifiedStarSelectItem;
import io.sqm.core.QuoteStyle;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QualifiedStarSelectItemParserTest {

    private final ParseContext ctx = ParseContext.of(new AnsiSpecs());

    @Test
    void quoted_qualifier_preserves_quote_style() {
        var result = ctx.parse(QualifiedStarSelectItem.class, "\"T\".*");
        assertTrue(result.ok(), () -> "problems: " + result.problems());

        var item = result.value();
        assertEquals("T", item.qualifier().value());
        assertEquals(QuoteStyle.DOUBLE_QUOTE, item.qualifier().quoteStyle());
    }
}
