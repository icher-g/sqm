package io.sqm.control;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReasonGuidanceCatalogTest {

    @Test
    void provides_retry_guidance_for_key_deny_reasons() {
        var reasons = List.of(
            ReasonCode.DENY_DDL,
            ReasonCode.DENY_DML,
            ReasonCode.DENY_TABLE,
            ReasonCode.DENY_COLUMN,
            ReasonCode.DENY_FUNCTION);

        for (var reason : reasons) {
            var guidance = ReasonGuidanceCatalog.forReason(reason);
            assertNotNull(guidance);
            assertTrue(guidance.retryable());
            assertFalse(guidance.remediationHint().isBlank());
            assertFalse(guidance.retryInstructionHint().isBlank());
        }
    }
}
