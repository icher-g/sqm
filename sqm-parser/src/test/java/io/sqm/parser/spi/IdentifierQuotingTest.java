package io.sqm.parser.spi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IdentifierQuotingTest {

    @Test
    void shouldCreateQuotingWithDoubleQuotes() {
        var quoting = IdentifierQuoting.of('"');
        
        assertTrue(quoting.supports('"'));
    }

    @Test
    void shouldCreateQuotingWithBackticks() {
        var quoting = IdentifierQuoting.of('`');
        
        assertTrue(quoting.supports('`'));
    }

    @Test
    void shouldCreateQuotingWithBrackets() {
        var quoting = IdentifierQuoting.of('[', ']');
        
        assertTrue(quoting.supports('['));
        assertTrue(quoting.supports(']'));
    }

    @Test
    void shouldSupportRecordEquality() {
        var quoting1 = IdentifierQuoting.of('"');
        var quoting2 = IdentifierQuoting.of('"');
        var quoting3 = IdentifierQuoting.of('`');
        
        assertEquals(quoting1, quoting2);
        assertNotEquals(quoting1, quoting3);
    }

    @Test
    void shouldSupportRecordHashCode() {
        var quoting1 = IdentifierQuoting.of('"');
        var quoting2 = IdentifierQuoting.of('"');
        
        assertEquals(quoting1.hashCode(), quoting2.hashCode());
    }

    @Test
    void shouldSupportRecordToString() {
        var quoting = IdentifierQuoting.of('"');
        
        String str = quoting.toString();
        assertNotNull(str);
    }
}
