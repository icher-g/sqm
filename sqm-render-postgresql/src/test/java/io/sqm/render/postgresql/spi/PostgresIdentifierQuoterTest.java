package io.sqm.render.postgresql.spi;

import io.sqm.core.QuoteStyle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostgresIdentifierQuoterTest {

    private final PostgresIdentifierQuoter quoter = new PostgresIdentifierQuoter();

    @Test
    void quoteWithStyleSupportsPostgresDoubleQuotesOnly() {
        assertEquals("\"select\"", quoter.quote("select", QuoteStyle.NONE));
        assertEquals("\"x\"", quoter.quote("x", QuoteStyle.DOUBLE_QUOTE));
        assertThrows(IllegalArgumentException.class, () -> quoter.quote("x", QuoteStyle.BACKTICK));
    }

    @Test
    void qualifyHandlesNullSchema() {
        assertEquals("\"t\"", quoter.qualify(null, "t"));
        assertEquals("\"s\".\"t\"", quoter.qualify("s", "t"));
    }

    @Test
    void supportsReportsExpectedStyles() {
        assertTrue(quoter.supports(null));
        assertTrue(quoter.supports(QuoteStyle.NONE));
        assertTrue(quoter.supports(QuoteStyle.DOUBLE_QUOTE));
        assertFalse(quoter.supports(QuoteStyle.BACKTICK));
        assertFalse(quoter.supports(QuoteStyle.BRACKETS));
    }
}

