package io.sqm.control;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReasonGuidanceCatalogTest {

    @Test
    void provides_retry_guidance_for_key_deny_reasons() {
        var reasons = List.of(
            ReasonCode.DENY_DDL,
            ReasonCode.DENY_DML,
            ReasonCode.DENY_TABLE,
            ReasonCode.DENY_COLUMN,
            ReasonCode.DENY_FUNCTION,
            ReasonCode.DENY_TENANT_REQUIRED);

        for (var reason : reasons) {
            var guidance = ReasonGuidanceCatalog.forReason(reason);
            assertNotNull(guidance);
            assertTrue(guidance.retryable());
            assertFalse(guidance.remediationHint().isBlank());
            assertFalse(guidance.retryInstructionHint().isBlank());
        }
    }

    @Test
    void provides_terminal_guidance_for_pipeline_error() {
        var guidance = ReasonGuidanceCatalog.forReason(ReasonCode.DENY_PIPELINE_ERROR);
        assertNotNull(guidance);
        assertFalse(guidance.retryable());
        assertFalse(guidance.remediationHint().isBlank());
        assertNull(guidance.retryInstructionHint());
    }

    @Test
    void returns_null_for_non_mapped_reason_and_rejects_null() {
        assertNull(ReasonGuidanceCatalog.forReason(ReasonCode.NONE));
        assertThrows(IllegalArgumentException.class, () -> ReasonGuidanceCatalog.forReason(null));
    }
}
