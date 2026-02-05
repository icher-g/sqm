package io.sqm.parser.ansi;

import io.sqm.parser.core.Token;
import io.sqm.parser.core.TokenType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link AnsiOperatorPolicy}.
 */
class AnsiOperatorPolicyTest {

    private final AnsiOperatorPolicy policy = new AnsiOperatorPolicy();

    @Test
    void recognizes_generic_binary_operator() {
        Token token = new Token(TokenType.OPERATOR, "||", 0);
        assertTrue(policy.isGenericBinaryOperator(token));
    }

    @Test
    void excludes_arithmetic_operator() {
        Token token = new Token(TokenType.OPERATOR, "+", 0);
        assertFalse(policy.isGenericBinaryOperator(token));
    }

    @Test
    void excludes_comparison_operator() {
        Token token = new Token(TokenType.OPERATOR, "=", 0);
        assertFalse(policy.isGenericBinaryOperator(token));
    }

    @Test
    void excludes_regex_operator() {
        Token token = new Token(TokenType.OPERATOR, "~", 0);
        assertFalse(policy.isGenericBinaryOperator(token));
    }

    @Test
    void accepts_qmark_operator_token() {
        Token token = new Token(TokenType.QMARK, "?", 0);
        assertTrue(policy.isGenericBinaryOperator(token));
    }
}
