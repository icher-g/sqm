package io.sqm.control;

import io.sqm.control.decision.ReasonCode;
import io.sqm.control.pipeline.StatementValidateResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatementValidateResultTest {

    @Test
    void ok_factory_returns_non_failed_result() {
        var result = StatementValidateResult.ok();

        assertEquals(ReasonCode.NONE, result.code());
        assertNull(result.message());
        assertFalse(result.isFailed());
    }

    @Test
    void failure_factory_returns_failed_result() {
        var result = StatementValidateResult.failure(ReasonCode.DENY_VALIDATION, "validation failed");

        assertEquals(ReasonCode.DENY_VALIDATION, result.code());
        assertEquals("validation failed", result.message());
        assertTrue(result.isFailed());
    }
}
