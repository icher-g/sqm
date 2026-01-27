package io.sqm.parser.core;

import io.sqm.parser.spi.IdentifierQuoting;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CursorTest {

    private final IdentifierQuoting identifierQuoting = ch -> ch == '"';

    @Test
    void of_createsCursorFromString() {
        Cursor cur = Cursor.of("SELECT * FROM t", identifierQuoting);
        assertNotNull(cur);
        assertEquals(TokenType.SELECT, cur.peek().type());
    }

    @Test
    void cursor_addEOFAutomatically() {
        List<Token> tokens = List.of(
            new Token(TokenType.SELECT, "SELECT", 0),
            new Token(TokenType.IDENT, "x", 7)
        );
        Cursor cur = new Cursor(tokens);

        assertEquals(3, cur.size()); // SELECT, x, EOF
        cur.advance();
        cur.advance();
        assertTrue(cur.isEof());
    }

    @Test
    void cursor_doesNotDuplicateEOF() {
        List<Token> tokens = List.of(
            new Token(TokenType.SELECT, "SELECT", 0),
            new Token(TokenType.EOF, "", 6)
        );
        Cursor cur = new Cursor(tokens);

        assertEquals(2, cur.size()); // SELECT, EOF (no duplicate)
    }

    @Test
    void peek_returnsCurrentToken() {
        Cursor cur = Cursor.of("SELECT * FROM", identifierQuoting);
        assertEquals(TokenType.SELECT, cur.peek().type());
        assertEquals(TokenType.SELECT, cur.peek().type()); // Still same
    }

    @Test
    void peek_withLookahead() {
        Cursor cur = Cursor.of("SELECT * FROM", identifierQuoting);
        assertEquals(TokenType.SELECT, cur.peek().type());
        assertEquals(TokenType.OPERATOR, cur.peek(1).type()); // *
        assertEquals(TokenType.FROM, cur.peek(2).type());
    }

    @Test
    void peek_throwsWhenOutOfBounds() {
        Cursor cur = Cursor.of("SELECT", identifierQuoting);
        assertThrows(NoSuchElementException.class, () -> cur.peek(10));
    }

    @Test
    void advance_returnsAndMovesForward() {
        Cursor cur = Cursor.of("SELECT *", identifierQuoting);
        Token t = cur.advance();
        assertEquals(TokenType.SELECT, t.type());
        assertEquals(TokenType.OPERATOR, cur.peek().type()); // Now at *
    }

    @Test
    void advance_throwsAtEnd() {
        Cursor cur = Cursor.of("SELECT", identifierQuoting);
        cur.advance(); // SELECT
        cur.advance(); // EOF
        assertThrows(NoSuchElementException.class, () -> cur.advance());
    }

    @Test
    void markAndRestore_allowsBacktracking() {
        Cursor cur = Cursor.of("SELECT * FROM", identifierQuoting);
        int mark = cur.mark();

        cur.advance(); // SELECT
        cur.advance(); // *
        assertEquals(TokenType.FROM, cur.peek().type());

        cur.restore(mark);
        assertEquals(TokenType.SELECT, cur.peek().type()); // Back to start
    }

    @Test
    void match_returnsTrueWhenTypesMatch() {
        Cursor cur = Cursor.of("SELECT *", identifierQuoting);
        assertTrue(cur.match(TokenType.SELECT));
        assertFalse(cur.match(TokenType.FROM));
    }

    @Test
    void match_withLookahead() {
        Cursor cur = Cursor.of("SELECT * FROM", identifierQuoting);
        assertTrue(cur.match(TokenType.OPERATOR, 1)); // *
        assertTrue(cur.match(TokenType.FROM, 2));
        assertFalse(cur.match(TokenType.WHERE, 10));
    }

    @Test
    void matchAny_withVarargs() {
        Cursor cur = Cursor.of("SELECT *", identifierQuoting);
        assertTrue(cur.matchAny(TokenType.SELECT, TokenType.FROM));
        assertFalse(cur.matchAny(TokenType.FROM, TokenType.WHERE));
    }

    @Test
    void matchAny_withSet() {
        Cursor cur = Cursor.of("SELECT *", identifierQuoting);
        assertTrue(cur.matchAny(Set.of(TokenType.SELECT, TokenType.FROM)));
        assertFalse(cur.matchAny(Set.of(TokenType.FROM, TokenType.WHERE)));
    }

    @Test
    void matchAny_withLookahead() {
        Cursor cur = Cursor.of("SELECT * FROM", identifierQuoting);
        assertTrue(cur.matchAny(Set.of(TokenType.OPERATOR, TokenType.COMMA), 1));
        assertFalse(cur.matchAny(Set.of(TokenType.WHERE), 10));
    }

    @Test
    void consumeIf_advancesWhenMatched() {
        Cursor cur = Cursor.of("SELECT *", identifierQuoting);
        assertTrue(cur.consumeIf(TokenType.SELECT));
        assertEquals(TokenType.OPERATOR, cur.peek().type()); // Advanced to *
    }

    @Test
    void consumeIf_doesNotAdvanceWhenNotMatched() {
        Cursor cur = Cursor.of("SELECT *", identifierQuoting);
        assertFalse(cur.consumeIf(TokenType.FROM));
        assertEquals(TokenType.SELECT, cur.peek().type()); // Still at SELECT
    }

    @Test
    void consumeIf_withPredicate() {
        Cursor cur = Cursor.of("SELECT *", identifierQuoting);
        assertTrue(cur.consumeIf(t -> t.type() == TokenType.SELECT));
        assertFalse(cur.consumeIf(t -> t.type() == TokenType.FROM));
    }

    @Test
    void expect_advancesWhenMatched() {
        Cursor cur = Cursor.of("SELECT *", identifierQuoting);
        Token t = cur.expect("Expected SELECT", TokenType.SELECT);
        assertEquals(TokenType.SELECT, t.type());
        assertEquals(TokenType.OPERATOR, cur.peek().type());
    }

    @Test
    void expect_throwsWhenNotMatched() {
        Cursor cur = Cursor.of("SELECT *", identifierQuoting);
        ParserException ex = assertThrows(ParserException.class,
            () -> cur.expect("Expected FROM", TokenType.FROM));
        assertTrue(ex.getMessage().contains("Expected FROM"));
    }

    @Test
    void expect_withSet() {
        Cursor cur = Cursor.of("SELECT *", identifierQuoting);
        Token t = cur.expect("Expected SELECT or UNION",
            Set.of(TokenType.SELECT, TokenType.UNION));
        assertEquals(TokenType.SELECT, t.type());
    }

    @Test
    void expect_withPredicate() {
        Cursor cur = Cursor.of("SELECT *", identifierQuoting);
        Token t = cur.expect("Expected keyword",
            token -> token.type() == TokenType.SELECT);
        assertEquals(TokenType.SELECT, t.type());
    }

    @Test
    void isEof_detectsEndOfFile() {
        Cursor cur = Cursor.of("SELECT", identifierQuoting);
        assertFalse(cur.isEof());
        cur.advance();
        assertTrue(cur.isEof());
    }

    @Test
    void pos_returnsCurrentPosition() {
        Cursor cur = Cursor.of("SELECT * FROM", identifierQuoting);
        assertEquals(0, cur.pos());
        cur.advance();
        assertEquals(1, cur.pos());
        cur.advance();
        assertEquals(2, cur.pos());
    }

    @Test
    void fullPos_includesBasePosition() {
        List<Token> tokens = List.of(
            new Token(TokenType.IDENT, "x", 0),
            new Token(TokenType.COMMA, ",", 1),
            new Token(TokenType.IDENT, "y", 2)
        );
        Cursor cur = new Cursor(tokens, 100);
        assertEquals(100, cur.fullPos());
        cur.advance();
        assertEquals(101, cur.fullPos());
    }

    @Test
    void advanceWithEnd_createsSubCursor() {
        Cursor cur = Cursor.of("SELECT * FROM t", identifierQuoting);
        Cursor sub = cur.advance(3); // Take first 3 tokens

        assertEquals(4, sub.size()); // SELECT, *, FROM, EOF (EOF added automatically)
        assertEquals(TokenType.SELECT, sub.peek().type());

        // Original cursor should now be at position 3
        assertEquals(TokenType.IDENT, cur.peek().type()); // t
    }

    @Test
    void advanceWithEnd_throwsOnInvalidRange() {
        Cursor cur = Cursor.of("SELECT *", identifierQuoting);
        assertThrows(IndexOutOfBoundsException.class, () -> cur.advance(10));
        assertThrows(IndexOutOfBoundsException.class, () -> cur.advance(-1));
    }

    @Test
    void removeBrackets_removesMatchingParentheses() {
        Cursor cur = Cursor.of("(SELECT * FROM t)", identifierQuoting);
        Cursor inner = cur.removeBrackets();

        assertEquals(TokenType.SELECT, inner.peek().type());
        // Check the original cursor advanced past both brackets
        assertTrue(cur.isEof());
    }

    @Test
    void removeBrackets_doesNothingWithoutBrackets() {
        Cursor cur = Cursor.of("SELECT * FROM t", identifierQuoting);
        Cursor same = cur.removeBrackets();

        assertSame(cur, same); // Should return same instance
        assertEquals(TokenType.SELECT, cur.peek().type());
    }

    @Test
    void removeBrackets_doesNothingWithUnmatchedBrackets() {
        Cursor cur = Cursor.of("(SELECT * FROM t", identifierQuoting);
        Cursor same = cur.removeBrackets();

        assertSame(cur, same); // Should return same instance
    }

    @Test
    void removeBrackets_doesNothingWithExtraTokensAfterRPAREN() {
        Cursor cur = Cursor.of("(SELECT *) FROM t", identifierQuoting);
        Cursor same = cur.removeBrackets();

        assertSame(cur, same); // Should return same instance
    }

    @Test
    void find_findsTokenAtTopLevel() {
        Cursor cur = Cursor.of("SELECT * FROM t WHERE x = 1", identifierQuoting);
        int idx = cur.find(TokenType.WHERE);
        assertTrue(idx < cur.size());
        assertEquals(TokenType.WHERE, cur.peek(idx).type());
    }

    @Test
    void find_returnsSizeWhenNotFound() {
        Cursor cur = Cursor.of("SELECT * FROM t", identifierQuoting);
        int idx = cur.find(TokenType.WHERE);
        assertEquals(cur.size(), idx);
    }

    @Test
    void find_skipsNestedParentheses() {
        Cursor cur = Cursor.of("SELECT * FROM (SELECT x, y FROM t) WHERE z = 1", identifierQuoting);
        int idx = cur.find(TokenType.WHERE);
        assertTrue(idx < cur.size());
        // The WHERE inside the subquery should be skipped
        Token whereToken = cur.peek(idx);
        assertEquals(TokenType.WHERE, whereToken.type());
    }

    @Test
    void find_withLookahead() {
        Cursor cur = Cursor.of("SELECT * FROM t WHERE", identifierQuoting);
        int idx = cur.find(1, TokenType.FROM); // Skip first token
        assertTrue(idx < cur.size());
    }

    @Test
    void find_withSet() {
        Cursor cur = Cursor.of("SELECT * FROM t", identifierQuoting);
        int idx = cur.find(Set.of(TokenType.FROM, TokenType.WHERE));
        assertTrue(idx < cur.size());
        assertEquals(TokenType.FROM, cur.peek(idx).type());
    }

    @Test
    void find_withPredicate() {
        Cursor cur = Cursor.of("SELECT * FROM t WHERE x = 1", identifierQuoting);
        int idx = cur.find(t -> OperatorTokens.isEq(t));
        assertTrue(idx < cur.size());
        assertTrue(OperatorTokens.isEq(cur.peek(idx)));
    }

    @Test
    void find_withStopLookup() {
        Cursor cur = Cursor.of("SELECT * FROM t WHERE x = 1", identifierQuoting);
        // Stop at WHERE, so FROM should be found
        int idx = cur.find(Set.of(TokenType.FROM), Set.of(TokenType.WHERE));
        assertTrue(idx < cur.size());
        assertEquals(TokenType.FROM, cur.peek(idx).type());
    }

    @Test
    void find_withCustomSkipTokens() {
        // Test finding with custom start/end skip tokens
        Cursor cur = Cursor.of("SELECT CASE WHEN x THEN y END FROM t", identifierQuoting);
        // This tests the full signature with custom skip boundaries
        int idx = cur.find(
            Set.of(TokenType.FROM),
            Set.of(TokenType.CASE),   // startSkip
            Set.of(TokenType.END),    // endSkip
            Set.of(TokenType.EOF),    // stopLookup
            0
        );
        assertTrue(idx < cur.size());
        assertEquals(TokenType.FROM, cur.peek(idx).type());
    }

    @Test
    void find_respectsNestedDepth() {
        Cursor cur = Cursor.of("SELECT (a, (b, c), d) FROM t", identifierQuoting);
        int idx = cur.find(TokenType.FROM);
        assertTrue(idx < cur.size());
        assertEquals(TokenType.FROM, cur.peek(idx).type());
        // Commas inside nested parens should be skipped
    }

    @Test
    void size_returnsTokenCount() {
        Cursor cur = Cursor.of("SELECT * FROM", identifierQuoting);
        assertEquals(4, cur.size()); // SELECT, *, FROM, EOF
    }

    @Test
    void emptyInput_createsEOFOnlyCursor() {
        Cursor cur = Cursor.of("", identifierQuoting);
        assertEquals(1, cur.size());
        assertTrue(cur.isEof());
    }

    @Test
    void multipleAdvances_work() {
        Cursor cur = Cursor.of("SELECT * FROM t WHERE", identifierQuoting);
        cur.advance(); // SELECT
        cur.advance(); // *
        cur.advance(); // FROM
        assertEquals(TokenType.IDENT, cur.peek().type()); // t
    }

    @Test
    void matchAny_withLookaheadVarargs() {
        Cursor cur = Cursor.of("SELECT * FROM", identifierQuoting);
        assertTrue(cur.matchAny(1, TokenType.OPERATOR, TokenType.COMMA));
        assertFalse(cur.matchAny(1, TokenType.SELECT, TokenType.FROM));
    }
}
