package io.sqm.parser.spi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link OperatorPolicy} defaults.
 */
public class OperatorPolicyTest {

    @Test
    void default_custom_operator_precedence_is_low() {
        OperatorPolicy policy = token -> false;

        assertEquals(OperatorPrecedence.CUSTOM_LOW, policy.customOperatorPrecedence("->"));
    }
}
