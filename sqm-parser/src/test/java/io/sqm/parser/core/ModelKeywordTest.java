package io.sqm.parser.core;

import io.sqm.parser.spi.IdentifierQuoting;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class ModelKeywordTest {
    @Test
    void recognizesModelKeywordsWithoutTreatingThemAsIdentifiers() {
        for (var word : ("RULES UPSERT UPDATED REFERENCE MAIN RETURN KEEP NAV UNIQUE DIMENSION SINGLE "
            + "AUTOMATIC SEQUENTIAL ITERATE UNTIL INCREMENT DECREMENT CV ITERATION_NUMBER "
            + "PRESENTV PRESENTNNV PREVIOUS PRESENT").split(" ")) {
            for (var spelling : new String[]{word, word.toLowerCase(Locale.ROOT)}) {
                var cur = Cursor.of(spelling, IdentifierQuoting.of('"'));
                assertEquals(TokenType.valueOf(word), cur.peek().type());
                assertFalse(cur.match(TokenType.IDENT));
                assertEquals(spelling, cur.expect("Expected keyword", TokenType.valueOf(word)).lexeme());
            }
        }
    }

    @Test
    void modelIsAClauseBoundaryAndQuotedKeywordsStayIdentifiers() {
        var cur = Cursor.of("MODEL", IdentifierQuoting.of('"'));
        assertEquals(TokenType.MODEL, cur.peek().type());
        assertFalse(cur.match(TokenType.IDENT));
        assertFalse(cur.matchAny(TokenType.IDENT, TokenType.NUMBER));
        assertThrows(ParserException.class, () -> cur.expect("Expected identifier", TokenType.IDENT));
        var quoted = Cursor.of("\"MODEL\" \"REFERENCE\"", IdentifierQuoting.of('"'));
        assertTrue(quoted.expect("identifier", TokenType.IDENT).quotedIdentifier());
        assertTrue(quoted.expect("identifier", TokenType.IDENT).quotedIdentifier());
    }
}
