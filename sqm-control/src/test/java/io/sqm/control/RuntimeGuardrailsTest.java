package io.sqm.control;

import io.sqm.control.audit.*;
import io.sqm.control.config.*;
import io.sqm.control.decision.*;
import io.sqm.control.execution.*;
import io.sqm.control.pipeline.*;
import io.sqm.control.rewrite.*;
import io.sqm.control.service.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RuntimeGuardrailsTest {

    @Test
    void disabled_has_no_limits() {
        var guardrails = RuntimeGuardrails.disabled();
        assertNull(guardrails.maxSqlLength());
        assertNull(guardrails.timeoutMillis());
        assertNull(guardrails.maxRows());
        assertFalse(guardrails.explainDryRun());
    }

    @Test
    void rejects_non_positive_limits() {
        assertThrows(IllegalArgumentException.class, () -> new RuntimeGuardrails(0, null, null, false));
        assertThrows(IllegalArgumentException.class, () -> new RuntimeGuardrails(null, 0L, null, false));
        assertThrows(IllegalArgumentException.class, () -> new RuntimeGuardrails(null, null, 0, false));
    }
}



