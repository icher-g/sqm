package io.cherlabs.sqlmodel.parser.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LexerQuotedIdentifiersTest {

    private static Token t(List<Token> ts, int i) { return ts.get(i); }

    @Test
    void ansi_double_quotes_in_function_and_collate() {
        String s = "LOWER(\"T\".\"Name\") NULLS FIRST COLLATE \"de-CH\" DESC";
        List<Token> toks = Lexer.lexAll(s);

        assertEquals(TokenType.IDENT, t(toks,0).type());  // LOWER
        assertEquals("LOWER", t(toks,0).lexeme());

        assertEquals(TokenType.LPAREN, t(toks,1).type());

        assertEquals(TokenType.IDENT, t(toks,2).type());  // "T" -> T
        assertEquals("T", t(toks,2).lexeme());

        assertEquals(TokenType.DOT, t(toks,3).type());

        assertEquals(TokenType.IDENT, t(toks,4).type());  // "Name" -> Name
        assertEquals("Name", t(toks,4).lexeme());

        assertEquals(TokenType.RPAREN, t(toks,5).type());

        assertEquals(TokenType.NULLS, t(toks,6).type());  // NULLS
        assertEquals("NULLS", t(toks,6).lexeme());

        assertEquals(TokenType.FIRST, t(toks,7).type());  // FIRST
        assertEquals("FIRST", t(toks,7).lexeme());

        assertEquals(TokenType.COLLATE, t(toks,8).type());  // COLLATE
        assertEquals("COLLATE", t(toks,8).lexeme());

        assertEquals(TokenType.IDENT, t(toks,9).type());  // "de-CH" -> de-CH
        assertEquals("de-CH", t(toks,9).lexeme());

        assertEquals(TokenType.DESC, t(toks,10).type()); // DESC
        assertEquals("DESC", t(toks,10).lexeme());

        assertEquals(TokenType.EOF, t(toks,11).type());
    }

    @Test
    void bracketed_identifiers_with_spaces_and_escape() {
        String s = "[dbo].[Order]]Name]";
        List<Token> toks = Lexer.lexAll(s);

        assertEquals(TokenType.IDENT, t(toks,0).type());  // [dbo] -> dbo
        assertEquals("dbo", t(toks,0).lexeme());

        assertEquals(TokenType.DOT, t(toks,1).type());

        assertEquals(TokenType.IDENT, t(toks,2).type());  // [Order]]Name] -> Order]Name
        assertEquals("Order]Name", t(toks,2).lexeme());

        assertEquals(TokenType.EOF, t(toks,3).type());
    }

    @Test
    void backtick_identifiers_with_escape() {
        String s = "`sch`.`ta``ble`";
        List<Token> toks = Lexer.lexAll(s);

        assertEquals(TokenType.IDENT, t(toks,0).type());  // `sch` -> sch
        assertEquals("sch", t(toks,0).lexeme());

        assertEquals(TokenType.DOT, t(toks,1).type());

        assertEquals(TokenType.IDENT, t(toks,2).type());  // `ta``ble` -> ta`ble
        assertEquals("ta`ble", t(toks,2).lexeme());

        assertEquals(TokenType.EOF, t(toks,3).type());
    }

    @Test
    void mixed_unquoted_and_quoted() {
        String s = "t.\"Select\".[Order] . `when`";
        List<Token> toks = Lexer.lexAll(s);

        assertEquals("t", t(toks,0).lexeme());
        assertEquals(TokenType.DOT, t(toks,1).type());
        assertEquals("Select", t(toks,2).lexeme()); // from "Select"
        assertEquals(TokenType.DOT, t(toks,3).type());
        assertEquals("Order", t(toks,4).lexeme());  // from [Order]
        assertEquals(TokenType.DOT, t(toks,5).type());
        assertEquals("when", t(toks,6).lexeme());   // from `when`
        assertEquals(TokenType.EOF, t(toks,7).type());
    }

    @Test
    void unterminated_quoted_identifiers_throw() {
        assertThrows(IllegalArgumentException.class, () -> Lexer.lexAll("\"abc"));
        assertThrows(IllegalArgumentException.class, () -> Lexer.lexAll("[abc"));
        assertThrows(IllegalArgumentException.class, () -> Lexer.lexAll("`abc"));
    }
}
