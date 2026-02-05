package io.sqm.parser.postgresql;

import io.sqm.parser.ansi.AnsiOperatorPolicy;
import io.sqm.parser.core.Token;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.postgresql.spi.PostgresOperatorPolicy;
import io.sqm.parser.spi.OperatorPrecedence;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link PostgresOperatorPolicy}.
 */
public class PostgresOperatorPolicyTest {

    @Test
    void custom_operator_precedence_tiers_are_applied() {
        var policy = new PostgresOperatorPolicy(new AnsiOperatorPolicy());

        assertEquals(OperatorPrecedence.CUSTOM_LOW, policy.customOperatorPrecedence(null));
        assertEquals(OperatorPrecedence.CUSTOM_LOW, policy.customOperatorPrecedence(""));
        assertEquals(OperatorPrecedence.CUSTOM_LOW, policy.customOperatorPrecedence("OPERATOR(pg_catalog.##)"));
        assertEquals(OperatorPrecedence.CUSTOM_LOW, policy.customOperatorPrecedence("||"));
        assertEquals(OperatorPrecedence.CUSTOM_MEDIUM, policy.customOperatorPrecedence("&&"));
        assertEquals(OperatorPrecedence.CUSTOM_HIGH, policy.customOperatorPrecedence("##"));
    }

    @Test
    void exponentiation_is_not_generic_binary_operator() {
        var policy = new PostgresOperatorPolicy(new AnsiOperatorPolicy());

        assertFalse(policy.isGenericBinaryOperator(new Token(TokenType.OPERATOR, "^", 0)));
        assertTrue(policy.isGenericBinaryOperator(new Token(TokenType.OPERATOR, "##", 0)));
    }
}
