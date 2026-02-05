package io.sqm.parser.spi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link OperatorPrecedence}.
 */
public class OperatorPrecedenceTest {

    @Test
    void levels_are_ordered() {
        assertEquals(1, OperatorPrecedence.CUSTOM_LOW.level());
        assertEquals(2, OperatorPrecedence.CUSTOM_MEDIUM.level());
        assertEquals(3, OperatorPrecedence.CUSTOM_HIGH.level());
    }
}
