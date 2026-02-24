package io.sqm.render.spi;

import io.sqm.core.QuoteStyle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdentifierQuoterDefaultsTest {

    @Test
    void defaultQuoteWithQuoteStyleDelegatesAsDocumented() {
        var q = new StubQuoter();

        assertEquals("ifNeeded:id", q.quote("id", null));
        assertEquals("ifNeeded:id", q.quote("id", QuoteStyle.NONE));
        assertEquals("quoted:id", q.quote("id", QuoteStyle.BACKTICK));
    }

    @Test
    void defaultSupportsAssumesAnsiDoubleQuotes() {
        var q = new StubQuoter();

        assertTrue(q.supports(null));
        assertTrue(q.supports(QuoteStyle.NONE));
        assertTrue(q.supports(QuoteStyle.DOUBLE_QUOTE));
        assertFalse(q.supports(QuoteStyle.BACKTICK));
        assertFalse(q.supports(QuoteStyle.BRACKETS));
    }

    private static final class StubQuoter implements IdentifierQuoter {
        @Override
        public String quote(String identifier) {
            return "quoted:" + identifier;
        }

        @Override
        public String quoteIfNeeded(String identifier) {
            return "ifNeeded:" + identifier;
        }

        @Override
        public String qualify(String schemaOrNull, String name) {
            return schemaOrNull == null ? name : schemaOrNull + "." + name;
        }

        @Override
        public boolean needsQuoting(String identifier) {
            return false;
        }
    }
}

