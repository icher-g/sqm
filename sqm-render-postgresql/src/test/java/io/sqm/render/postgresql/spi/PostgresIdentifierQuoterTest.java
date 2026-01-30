package io.sqm.render.postgresql.spi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostgresIdentifierQuoterTest {

    private final PostgresIdentifierQuoter quoter = new PostgresIdentifierQuoter();

    @Test
    void quoteEscapesEmbeddedQuotes() {
        assertEquals("\"a\"\"b\"", quoter.quote("a\"b"));
    }

    @Test
    void quoteIfNeededLeavesSimpleIdentifiers() {
        assertEquals("col_1", quoter.quoteIfNeeded("col_1"));
    }

    @Test
    void quoteIfNeededQuotesReservedWords() {
        assertEquals("\"select\"", quoter.quoteIfNeeded("select"));
    }

    @Test
    void needsQuotingForInvalidIdentifiers() {
        assertTrue(quoter.needsQuoting("1col"));
        assertTrue(quoter.needsQuoting("has-dash"));
        assertTrue(quoter.needsQuoting(""));
        assertTrue(quoter.needsQuoting(null));
    }

    @Test
    void qualifyQuotesParts() {
        assertEquals("\"s\".\"t\"", quoter.qualify("s", "t"));
        assertEquals("\"t\"", quoter.qualify(null, "t"));
    }
}
