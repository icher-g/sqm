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
        assertTrue(quoter.supports(null));
        assertFalse(quoter.supports(QuoteStyle.DOUBLE_QUOTE));
        assertEquals("`order`", quoter.quote("order"));
    }

    @Test
    void quote_escapes_backticks_and_qualify_quotes_parts() {
        var quoter = new MySqlIdentifierQuoter();

        assertEquals("`a``b`", quoter.quote("a`b"));
        assertEquals("`t`", quoter.qualify(null, "t"));
        assertEquals("`t`", quoter.qualify("", "t"));
        assertEquals("`s`.`t`", quoter.qualify("s", "t"));
    }

    @Test
    void needsQuoting_and_quoteIfNeeded_cover_all_branches() {
        var quoter = new MySqlIdentifierQuoter();

        assertTrue(quoter.needsQuoting(null));
        assertTrue(quoter.needsQuoting(""));
        assertTrue(quoter.needsQuoting("a-b"));
        assertTrue(quoter.needsQuoting("key"));
        assertFalse(quoter.needsQuoting("abc_1"));

        assertEquals("abc_1", quoter.quoteIfNeeded("abc_1"));
        assertEquals("`key`", quoter.quoteIfNeeded("key"));
    }

    @Test
    void quote_style_none_and_backtick_paths() {
        var quoter = new MySqlIdentifierQuoter();

        assertEquals("`key`", quoter.quote("key", QuoteStyle.NONE));
        assertEquals("abc", quoter.quote("abc", QuoteStyle.NONE));
        assertEquals("`abc`", quoter.quote("abc", QuoteStyle.BACKTICK));
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
        assertEquals("\"a\"\"b\"", quoter.quote("a\"b", QuoteStyle.DOUBLE_QUOTE));
    }
}

