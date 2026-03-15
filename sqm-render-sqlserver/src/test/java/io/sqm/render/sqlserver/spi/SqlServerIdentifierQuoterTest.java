package io.sqm.render.sqlserver.spi;

import io.sqm.core.QuoteStyle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlServerIdentifierQuoterTest {

    @Test
    void quotes_with_brackets_by_default() {
        var quoter = new SqlServerIdentifierQuoter();

        assertEquals("[users]", quoter.quote("users"));
        assertTrue(quoter.supports(QuoteStyle.BRACKETS));
        assertFalse(quoter.supports(QuoteStyle.DOUBLE_QUOTE));
    }

    @Test
    void supports_double_quotes_when_quoted_identifier_mode_is_enabled() {
        var quoter = new SqlServerIdentifierQuoter(true);

        assertEquals("\"users\"", quoter.quote("users", QuoteStyle.DOUBLE_QUOTE));
        assertTrue(quoter.supports(QuoteStyle.DOUBLE_QUOTE));
    }

    @Test
    void rejects_double_quotes_when_mode_is_disabled() {
        var quoter = new SqlServerIdentifierQuoter();

        assertThrows(IllegalArgumentException.class, () -> quoter.quote("users", QuoteStyle.DOUBLE_QUOTE));
    }

    @Test
    void quotes_only_when_needed_and_qualifies_names() {
        var quoter = new SqlServerIdentifierQuoter();

        assertEquals("users", quoter.quote("users", QuoteStyle.NONE));
        assertEquals("[TOP]", quoter.quoteIfNeeded("TOP"));
        assertEquals("[dbo].[users]", quoter.qualify("dbo", "users"));
        assertEquals("[users]", quoter.qualify(null, "users"));
    }

    @Test
    void reports_needs_quoting_and_rejects_backticks() {
        var quoter = new SqlServerIdentifierQuoter();

        assertTrue(quoter.needsQuoting("has space"));
        assertTrue(quoter.needsQuoting(""));
        assertFalse(quoter.needsQuoting("users"));
        assertThrows(IllegalArgumentException.class, () -> quoter.quote("users", QuoteStyle.BACKTICK));
    }
}
