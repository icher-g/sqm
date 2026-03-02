package io.sqm.control;

import io.sqm.control.decision.DecisionExplanation;
import io.sqm.control.decision.DecisionResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class DecisionExplanationTest {

    @Test
    void rejects_blank_explanation() {
        assertThrows(IllegalArgumentException.class,
            () -> new DecisionExplanation(DecisionResult.allow(), " "));
    }

    @Test
    void rejects_null_decision() {
        assertThrows(NullPointerException.class,
            () -> new DecisionExplanation(null, "ok"));
    }
}


