package io.sqm.render.mysql.spi;

import io.sqm.core.QuoteStyle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MySqlIdentifierQuoterTest {

    @Test
    void supports_backtick_by_default() {
        var quoter = new MySqlIdentifierQuoter();

        assertTrue(quoter.supports(QuoteStyle.BACKTICK));
        assertTrue(quoter.supports(QuoteStyle.NONE));
        assertFalse(quoter.supports(QuoteStyle.DOUBLE_QUOTE));
        assertEquals("`order`", quoter.quote("order"));
    }

    @Test
    void double_quote_requires_ansi_quotes_mode() {
        var quoter = new MySqlIdentifierQuoter();

        assertThrows(IllegalArgumentException.class, () -> quoter.quote("id", QuoteStyle.DOUBLE_QUOTE));
    }

    @Test
    void supports_double_quote_when_ansi_quotes_mode_enabled() {
        var quoter = new MySqlIdentifierQuoter(true);

        assertTrue(quoter.supports(QuoteStyle.DOUBLE_QUOTE));
        assertEquals("\"id\"", quoter.quote("id", QuoteStyle.DOUBLE_QUOTE));
    }
}
