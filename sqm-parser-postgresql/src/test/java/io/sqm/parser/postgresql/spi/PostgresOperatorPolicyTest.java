package io.sqm.parser.postgresql.spi;

import io.sqm.parser.ansi.AnsiOperatorPolicy;
import io.sqm.parser.core.Token;
import io.sqm.parser.core.TokenType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link PostgresOperatorPolicy}.
 */
class PostgresOperatorPolicyTest {

    private final PostgresOperatorPolicy policy = new PostgresOperatorPolicy(new AnsiOperatorPolicy());

    @Test
    void excludes_exponentiation_operator_from_generic_binary() {
        Token token = new Token(TokenType.OPERATOR, "^", 0);
        assertFalse(policy.isGenericBinaryOperator(token));
    }

    @Test
    void preserves_generic_operator_classification_from_ansi_policy() {
        Token token = new Token(TokenType.OPERATOR, "||", 0);
        assertTrue(policy.isGenericBinaryOperator(token));
    }

    @Test
    void excludes_arithmetic_operator() {
        Token token = new Token(TokenType.OPERATOR, "+", 0);
        assertFalse(policy.isGenericBinaryOperator(token));
    }

    @Test
    void accepts_qmark_token_as_generic_operator() {
        Token token = new Token(TokenType.QMARK, "?", 0);
        assertTrue(policy.isGenericBinaryOperator(token));
    }
}
