package io.sqm.control;

import io.sqm.control.decision.DecisionGuidance;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    void retryable_factory_populates_fields() {
        var guidance = DecisionGuidance.retryable("fix sql", "rewrite_query", "remove ddl");
        assertTrue(guidance.retryable());
        assertEquals("fix sql", guidance.remediationHint());
        assertEquals("rewrite_query", guidance.suggestedAction());
        assertEquals("remove ddl", guidance.retryInstructionHint());
    }

    @Test
    void terminal_factory_populates_fields_with_null_retry_hint() {
        var guidance = DecisionGuidance.terminal("fix sql", "rewrite_query");
        assertFalse(guidance.retryable());
        assertEquals("fix sql", guidance.remediationHint());
        assertEquals("rewrite_query", guidance.suggestedAction());
        assertNull(guidance.retryInstructionHint());
    }
}


