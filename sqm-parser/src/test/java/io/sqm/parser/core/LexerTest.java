package io.sqm.parser.core;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LexerTest {

    private static Token t(List<Token> ts, int i) {
        return ts.get(i);
    }

    @Test
    void ansi_double_quotes_in_function_and_collate() {
        String s = "LOWER(\"T\".\"Name\") NULLS FIRST COLLATE \"de-CH\" DESC";
        List<Token> toks = Lexer.lexAll(s);

        assertEquals(TokenType.IDENT, t(toks, 0).type());  // LOWER
        assertEquals("LOWER", t(toks, 0).lexeme());

        assertEquals(TokenType.LPAREN, t(toks, 1).type());

        assertEquals(TokenType.IDENT, t(toks, 2).type());  // "T" -> T
        assertEquals("T", t(toks, 2).lexeme());

        assertEquals(TokenType.DOT, t(toks, 3).type());

        assertEquals(TokenType.IDENT, t(toks, 4).type());  // "Name" -> Name
        assertEquals("Name", t(toks, 4).lexeme());

        assertEquals(TokenType.RPAREN, t(toks, 5).type());

        assertEquals(TokenType.NULLS, t(toks, 6).type());  // NULLS
        assertEquals("NULLS", t(toks, 6).lexeme());

        assertEquals(TokenType.FIRST, t(toks, 7).type());  // FIRST
        assertEquals("FIRST", t(toks, 7).lexeme());

        assertEquals(TokenType.COLLATE, t(toks, 8).type());  // COLLATE
        assertEquals("COLLATE", t(toks, 8).lexeme());

        assertEquals(TokenType.IDENT, t(toks, 9).type());  // "de-CH" -> de-CH
        assertEquals("de-CH", t(toks, 9).lexeme());

        assertEquals(TokenType.DESC, t(toks, 10).type()); // DESC
        assertEquals("DESC", t(toks, 10).lexeme());

        assertEquals(TokenType.EOF, t(toks, 11).type());
    }

    @Test
    void bracketed_identifiers_with_spaces_and_escape() {
        String s = "[dbo].[Order]]Name]";
        List<Token> toks = Lexer.lexAll(s);

        assertEquals(TokenType.IDENT, t(toks, 0).type());  // [dbo] -> dbo
        assertEquals("dbo", t(toks, 0).lexeme());

        assertEquals(TokenType.DOT, t(toks, 1).type());

        assertEquals(TokenType.IDENT, t(toks, 2).type());  // [Order]]Name] -> Order]Name
        assertEquals("Order]Name", t(toks, 2).lexeme());

        assertEquals(TokenType.EOF, t(toks, 3).type());
    }

    @Test
    void backtick_identifiers_with_escape() {
        String s = "`sch`.`ta``ble`";
        List<Token> toks = Lexer.lexAll(s);

        assertEquals(TokenType.IDENT, t(toks, 0).type());  // `sch` -> sch
        assertEquals("sch", t(toks, 0).lexeme());

        assertEquals(TokenType.DOT, t(toks, 1).type());

        assertEquals(TokenType.IDENT, t(toks, 2).type());  // `ta``ble` -> ta`ble
        assertEquals("ta`ble", t(toks, 2).lexeme());

        assertEquals(TokenType.EOF, t(toks, 3).type());
    }

    @Test
    void mixed_unquoted_and_quoted() {
        String s = "t.\"Select\".[Order] . `when`";
        List<Token> toks = Lexer.lexAll(s);

        assertEquals("t", t(toks, 0).lexeme());
        assertEquals(TokenType.DOT, t(toks, 1).type());
        assertEquals("Select", t(toks, 2).lexeme()); // from "Select"
        assertEquals(TokenType.DOT, t(toks, 3).type());
        assertEquals("Order", t(toks, 4).lexeme());  // from [Order]
        assertEquals(TokenType.DOT, t(toks, 5).type());
        assertEquals("when", t(toks, 6).lexeme());   // from `when`
        assertEquals(TokenType.EOF, t(toks, 7).type());
    }

    @Test
    void unterminated_quoted_identifiers_throw() {
        assertThrows(IllegalArgumentException.class, () -> Lexer.lexAll("\"abc"));
        assertThrows(IllegalArgumentException.class, () -> Lexer.lexAll("[abc"));
        assertThrows(IllegalArgumentException.class, () -> Lexer.lexAll("`abc"));
    }

    // --- POSITIONAL: $n ----------------------------------------------------

    @Test
    void positionalParameterWithoutSpacesIsRecognized() {
        List<Token> tokens = Lexer.lexAll("SELECT * FROM t WHERE a = $1 AND b = $12");

        List<Token> params = filterTokensOfType(tokens, TokenType.PARAM_POS);
        assertEquals(2, params.size());

        assertEquals("1", params.get(0).lexeme());   // or value(), adapt to your API
        assertEquals("12", params.get(1).lexeme());
    }

    @Test
    void positionalParameterWithSpacesIsNotRecognized() {
        List<Token> tokens = Lexer.lexAll("SELECT * FROM t WHERE a = $ 1");

        // We don't care exactly how '$ 1' is tokenised,
        // we only assert that no POSITIONAL_PARAM token was produced.
        assertEquals(0, countTokensOfType(tokens, TokenType.PARAM_POS));
    }

    // --- QUESTION: ? -------------------------------------------------------

    @Test
    void questionMarkParameterIsRecognized() {
        List<Token> tokens = Lexer.lexAll("SELECT * FROM t WHERE a = ? AND b = ?");

        List<Token> params = filterTokensOfType(tokens, TokenType.QMARK);
        assertEquals(2, params.size());
        assertEquals("?", params.get(0).lexeme());
        assertEquals("?", params.get(1).lexeme());
    }

    // --- NAMED: :name ------------------------------------------------------

    @Test
    void namedParameterWithColonIsRecognized() {
        List<Token> tokens = Lexer.lexAll("SELECT * FROM t WHERE a = :id AND b = :user_name");

        List<Token> params = filterTokensOfType(tokens, TokenType.PARAM_NAMED);
        assertEquals(2, params.size());

        assertEquals("id", params.get(0).lexeme());
        assertEquals("user_name", params.get(1).lexeme());
    }

    @Test
    void namedParameterWithColonAndSpaceIsNotRecognized() {
        assertThrows(ParserException.class, () -> Lexer.lexAll("SELECT * FROM t WHERE a = : id"));
    }

    // --- NAMED: @name  (user var vs param – lexer just recognises the shape) ---

    @Test
    void namedParameterWithAtIsRecognizedByCoreLexer() {
        List<Token> tokens = Lexer.lexAll("SELECT * FROM t WHERE a = @id AND b = @user_name");

        List<Token> params = filterTokensOfType(tokens, TokenType.PARAM_NAMED);
        assertEquals(2, params.size());

        assertEquals("id", params.get(0).lexeme());
        assertEquals("user_name", params.get(1).lexeme());
    }

    @Test
    void atFollowedByDigitIsNotRecognizedAsNamedParameter() {
        // Pattern for named params is @[A-Za-z_][A-Za-z0-9_]*
        assertThrows(ParserException.class, () -> Lexer.lexAll("SELECT * FROM t WHERE a = @1"));
    }

    // --- Mixed styles sanity test ------------------------------------------

    @Test
    void mixedParameterStylesAreLexedCorrectly() {
        List<Token> tokens = Lexer.lexAll("SELECT * FROM t WHERE a = $1 AND b = :name AND c = ? AND d = @id");

        long positional = countTokensOfType(tokens, TokenType.PARAM_POS);
        long named = countTokensOfType(tokens, TokenType.PARAM_NAMED);
        long question = countTokensOfType(tokens, TokenType.QMARK);

        assertEquals(1, positional);
        assertEquals(2, named);       // :name, @id
        assertEquals(1, question);
    }

    // --- No false positives in plain identifiers / punctuation --------------

    @Test
    void plainIdentifiersDoNotProduceParameterTokens() {
        List<Token> tokens = Lexer.lexAll("SELECT price, total, name FROM test");

        EnumSet<TokenType> paramTypes = EnumSet.of(
            TokenType.PARAM_POS,
            TokenType.PARAM_NAMED,
            TokenType.QMARK
        );

        long paramCount = tokens.stream()
            .filter(t -> paramTypes.contains(t.type()))
            .count();

        assertEquals(0, paramCount);
    }

    private long countTokensOfType(List<Token> tokens, TokenType type) {
        return tokens.stream().filter(t -> t.type() == type).count();
    }

    private List<Token> filterTokensOfType(List<Token> tokens, TokenType type) {
        return tokens.stream().filter(t -> t.type() == type).toList();
    }

    // --- Additional comprehensive tests ---

    @Test
    void lexer_handlesEmptyString() {
        List<Token> tokens = Lexer.lexAll("");
        assertEquals(1, tokens.size());
        assertEquals(TokenType.EOF, tokens.get(0).type());
    }

    @Test
    void lexer_handlesWhitespaceOnly() {
        List<Token> tokens = Lexer.lexAll("   \t\n  ");
        assertEquals(1, tokens.size());
        assertEquals(TokenType.EOF, tokens.get(0).type());
    }

    @Test
    void lexer_handlesLineComments() {
        List<Token> tokens = Lexer.lexAll("SELECT -- this is a comment\n * FROM t");
        assertEquals(TokenType.SELECT, tokens.get(0).type());
        assertEquals(TokenType.OPERATOR, tokens.get(1).type()); // *
        assertEquals(TokenType.FROM, tokens.get(2).type());
    }

    @Test
    void lexer_handlesBlockComments() {
        List<Token> tokens = Lexer.lexAll("SELECT /* comment */ * FROM t");
        assertEquals(TokenType.SELECT, tokens.get(0).type());
        assertEquals(TokenType.OPERATOR, tokens.get(1).type()); // *
        assertEquals(TokenType.FROM, tokens.get(2).type());
    }

    @Test
    void lexer_throwsOnUnterminatedBlockComment() {
        assertThrows(ParserException.class, () -> Lexer.lexAll("SELECT /* unterminated"));
    }

    @Test
    void lexer_handlesMultilineBlockComment() {
        List<Token> tokens = Lexer.lexAll("SELECT /*\n multi\n line\n comment\n*/ * FROM t");
        assertEquals(TokenType.SELECT, tokens.get(0).type());
        assertEquals(TokenType.OPERATOR, tokens.get(1).type());
    }

    @Test
    void lexer_handlesStringLiteralsWithEscapes() {
        List<Token> tokens = Lexer.lexAll("'hello''world'");
        assertEquals(1, countTokensOfType(tokens, TokenType.STRING));
        assertEquals("hello'world", tokens.get(0).lexeme());
    }

    @Test
    void lexer_handlesEmptyStringLiteral() {
        List<Token> tokens = Lexer.lexAll("''");
        assertEquals(1, countTokensOfType(tokens, TokenType.STRING));
        assertEquals("", tokens.get(0).lexeme());
    }

    @Test
    void lexer_throwsOnUnterminatedString() {
        assertThrows(ParserException.class, () -> Lexer.lexAll("'unterminated"));
    }

    @Test
    void lexer_handlesIntegerNumbers() {
        List<Token> tokens = Lexer.lexAll("42 100 0");
        assertEquals(3, countTokensOfType(tokens, TokenType.NUMBER));
        assertEquals("42", tokens.get(0).lexeme());
        assertEquals("100", tokens.get(1).lexeme());
        assertEquals("0", tokens.get(2).lexeme());
    }

    @Test
    void lexer_handlesDecimalNumbers() {
        List<Token> tokens = Lexer.lexAll("3.14 0.5 .5 5.");
        assertEquals(4, countTokensOfType(tokens, TokenType.NUMBER));
        assertEquals("3.14", tokens.get(0).lexeme());
    }

    @Test
    void lexer_handlesScientificNotation() {
        List<Token> tokens = Lexer.lexAll("1e10 2.5E-3 3.0e+5");
        assertEquals(3, countTokensOfType(tokens, TokenType.NUMBER));
        assertEquals("1e10", tokens.get(0).lexeme());
        assertEquals("2.5E-3", tokens.get(1).lexeme());
        assertEquals("3.0e+5", tokens.get(2).lexeme());
    }

    @Test
    void lexer_handlesAllArithmeticOperators() {
        List<Token> tokens = Lexer.lexAll("+ - * / %");
        assertEquals(5, countTokensOfType(tokens, TokenType.OPERATOR));
        assertEquals("+", tokens.get(0).lexeme());
        assertEquals("-", tokens.get(1).lexeme());
        assertEquals("*", tokens.get(2).lexeme());
        assertEquals("/", tokens.get(3).lexeme());
        assertEquals("%", tokens.get(4).lexeme());
    }

    @Test
    void lexer_handlesAllComparisonOperators() {
        List<Token> tokens = Lexer.lexAll("= <> != < <= > >=");
        assertEquals(7, countTokensOfType(tokens, TokenType.OPERATOR));
        assertEquals("=", tokens.get(0).lexeme());
        assertEquals("<>", tokens.get(1).lexeme());
        assertEquals("!=", tokens.get(2).lexeme());
        assertEquals("<", tokens.get(3).lexeme());
        assertEquals("<=", tokens.get(4).lexeme());
        assertEquals(">", tokens.get(5).lexeme());
        assertEquals(">=", tokens.get(6).lexeme());
    }

    @Test
    void lexer_handlesJsonOperators() {
        List<Token> tokens = Lexer.lexAll("-> ->> #> #>>");
        assertEquals(4, countTokensOfType(tokens, TokenType.OPERATOR));
        assertEquals("->", tokens.get(0).lexeme());
        assertEquals("->>", tokens.get(1).lexeme());
        assertEquals("#>", tokens.get(2).lexeme());
        assertEquals("#>>", tokens.get(3).lexeme());
    }

    @Test
    void lexer_handlesContainmentOperators() {
        List<Token> tokens = Lexer.lexAll("@> <@");
        assertEquals(2, countTokensOfType(tokens, TokenType.OPERATOR));
        assertEquals("@>", tokens.get(0).lexeme());
        assertEquals("<@", tokens.get(1).lexeme());
    }

    @Test
    void lexer_handlesConcatenationOperator() {
        List<Token> tokens = Lexer.lexAll("||");
        assertEquals(1, countTokensOfType(tokens, TokenType.OPERATOR));
        assertEquals("||", tokens.get(0).lexeme());
    }

    @Test
    void lexer_handlesArrayOverlapOperator() {
        List<Token> tokens = Lexer.lexAll("&&");
        assertEquals(1, countTokensOfType(tokens, TokenType.OPERATOR));
        assertEquals("&&", tokens.get(0).lexeme());
    }

    @Test
    void lexer_handlesRegexOperators() {
        List<Token> tokens = Lexer.lexAll("~ ~* !~ !~*");
        assertEquals(4, countTokensOfType(tokens, TokenType.OPERATOR));
        assertEquals("~", tokens.get(0).lexeme());
        assertEquals("~*", tokens.get(1).lexeme());
        assertEquals("!~", tokens.get(2).lexeme());
        assertEquals("!~*", tokens.get(3).lexeme());
    }

    @Test
    void lexer_handlesPunctuation() {
        List<Token> tokens = Lexer.lexAll("( ) , .");
        assertEquals(TokenType.LPAREN, tokens.get(0).type());
        assertEquals(TokenType.RPAREN, tokens.get(1).type());
        assertEquals(TokenType.COMMA, tokens.get(2).type());
        assertEquals(TokenType.DOT, tokens.get(3).type());
    }

    @Test
    void lexer_recognizesAllKeywords() {
        String keywords = "SELECT FROM WHERE GROUP BY HAVING ORDER LIMIT OFFSET " +
                         "JOIN INNER LEFT RIGHT FULL OUTER CROSS NATURAL ON USING " +
                         "AND OR NOT IN LIKE BETWEEN IS NULL TRUE FALSE " +
                         "CASE WHEN THEN ELSE END DISTINCT EXISTS AS " +
                         "UNION INTERSECT EXCEPT ALL ANY " +
                         "CAST ARRAY FILTER OVER PARTITION WINDOW";
        List<Token> tokens = Lexer.lexAll(keywords);
        
        // All should be recognized as keywords, not identifiers
        long keywordCount = tokens.stream()
            .filter(t -> t.type() != TokenType.IDENT && t.type() != TokenType.EOF)
            .count();
        assertTrue(keywordCount > 30, "Should recognize many keywords");
    }

    @Test
    void lexer_handlesIdentifiersWithUnderscoreAndDollar() {
        List<Token> tokens = Lexer.lexAll("_id $price user_name $1dollar");
        assertEquals(TokenType.IDENT, tokens.get(0).type());
        assertEquals("_id", tokens.get(0).lexeme());
        assertEquals(TokenType.IDENT, tokens.get(1).type());
        assertEquals("$price", tokens.get(1).lexeme());
    }

    @Test
    void lexer_throwsOnUnexpectedCharacter() {
        assertThrows(ParserException.class, () -> Lexer.lexAll("SELECT \u00A7")); // § character
    }

    @Test
    void lexer_handlesQuestionMarkJsonOperators() {
        List<Token> tokens = Lexer.lexAll("?| ?&");
        assertEquals(2, countTokensOfType(tokens, TokenType.OPERATOR));
        assertEquals("?|", tokens.get(0).lexeme());
        assertEquals("?&", tokens.get(1).lexeme());
    }

    @Test
    void lexer_distinguishesQuestionMarkVsJsonOperator() {
        List<Token> tokens = Lexer.lexAll("? ?| ?&");
        assertEquals(TokenType.QMARK, tokens.get(0).type());
        assertEquals(TokenType.OPERATOR, tokens.get(1).type());
        assertEquals("?|", tokens.get(1).lexeme());
        assertEquals(TokenType.OPERATOR, tokens.get(2).type());
        assertEquals("?&", tokens.get(2).lexeme());
    }

    @Test
    void lexer_handlesCaretOperator() {
        List<Token> tokens = Lexer.lexAll("2 ^ 3");
        assertEquals(TokenType.NUMBER, tokens.get(0).type());
        assertEquals(TokenType.OPERATOR, tokens.get(1).type());
        assertEquals("^", tokens.get(1).lexeme());
        assertEquals(TokenType.NUMBER, tokens.get(2).type());
    }

    @Test
    void lexer_handlesComplexQuery() {
        String sql = "SELECT t.id, t.name FROM users t WHERE t.age >= 18 AND t.status = 'active'";
        List<Token> tokens = Lexer.lexAll(sql);
        
        assertTrue(tokens.size() > 10);
        assertEquals(TokenType.SELECT, tokens.get(0).type());
        assertNotNull(tokens.stream().filter(t -> t.type() == TokenType.FROM).findFirst().orElse(null));
        assertNotNull(tokens.stream().filter(t -> t.type() == TokenType.WHERE).findFirst().orElse(null));
    }

    @Test
    void lexer_preservesTokenPositions() {
        String sql = "SELECT * FROM";
        List<Token> tokens = Lexer.lexAll(sql);
        
        assertEquals(0, tokens.get(0).pos()); // SELECT at position 0
        assertTrue(tokens.get(1).pos() > 0);   // * somewhere after SELECT
        assertTrue(tokens.get(2).pos() > tokens.get(1).pos()); // FROM after *
    }

    @Test
    void lexer_handlesConsecutiveOperators() {
        // Test that operators are lexed correctly when adjacent
        List<Token> tokens = Lexer.lexAll("a<>b");
        assertEquals(TokenType.IDENT, tokens.get(0).type());
        assertEquals(TokenType.OPERATOR, tokens.get(1).type());
        assertEquals("<>", tokens.get(1).lexeme());
        assertEquals(TokenType.IDENT, tokens.get(2).type());
    }

    @Test
    void lexer_dollarSignAloneIsIdentifier() {
        // $ alone (not followed by digit or letter) should be treated as identifier start
        List<Token> tokens = Lexer.lexAll("$price");
        assertEquals(TokenType.IDENT, tokens.get(0).type());
        assertEquals("$price", tokens.get(0).lexeme());
    }

    @Test
    void lexer_handlesMixedCaseKeywords() {
        List<Token> tokens = Lexer.lexAll("SeLeCt FrOm WhErE");
        assertEquals(TokenType.SELECT, tokens.get(0).type());
        assertEquals(TokenType.FROM, tokens.get(1).type());
        assertEquals(TokenType.WHERE, tokens.get(2).type());
    }
}
