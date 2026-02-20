package io.sqm.core.control;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class DecisionGuidanceTest {

    @Test
    void retryable_requires_retry_instruction_hint() {
        assertThrows(IllegalArgumentException.class,
            () -> DecisionGuidance.retryable("hint", "action", " "));
    }

    @Test
    void requires_remediation_hint() {
        assertThrows(IllegalArgumentException.class,
            () -> DecisionGuidance.terminal(" ", "action"));
    }

    @Test
    void requires_suggested_action() {
        assertThrows(IllegalArgumentException.class,
            () -> DecisionGuidance.terminal("hint", " "));
    }
}
