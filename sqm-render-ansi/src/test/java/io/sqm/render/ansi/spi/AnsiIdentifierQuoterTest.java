package io.sqm.render.ansi.spi;

import io.sqm.core.QuoteStyle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnsiIdentifierQuoterTest {

    private final AnsiIdentifierQuoter quoter = new AnsiIdentifierQuoter();

    @Test
    void quoteWithStyleSupportsAnsiDoubleQuotesOnly() {
        assertEquals("\"select\"", quoter.quote("select", QuoteStyle.NONE));
        assertEquals("\"x\"", quoter.quote("x", QuoteStyle.DOUBLE_QUOTE));
        assertThrows(IllegalArgumentException.class, () -> quoter.quote("x", QuoteStyle.BACKTICK));
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

