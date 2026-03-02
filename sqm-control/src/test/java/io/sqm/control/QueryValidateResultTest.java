package io.sqm.control;

import io.sqm.control.audit.*;
import io.sqm.control.config.*;
import io.sqm.control.decision.*;
import io.sqm.control.execution.*;
import io.sqm.control.pipeline.*;
import io.sqm.control.rewrite.*;
import io.sqm.control.service.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QueryValidateResultTest {

    @Test
    void ok_factory_returns_non_failed_result() {
        var result = QueryValidateResult.ok();

        assertEquals(ReasonCode.NONE, result.code());
        assertNull(result.message());
        assertFalse(result.isFailed());
    }

    @Test
    void failure_factory_returns_failed_result() {
        var result = QueryValidateResult.failure(ReasonCode.DENY_VALIDATION, "validation failed");

        assertEquals(ReasonCode.DENY_VALIDATION, result.code());
        assertEquals("validation failed", result.message());
        assertTrue(result.isFailed());
    }
}


