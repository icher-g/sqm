package io.sqm.control;

import io.sqm.core.transform.IdentifierNormalizationCaseMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BuiltInRewriteSettingsTest {

    @Test
    void defaults_and_null_mode_fallback_to_deny() {
        var defaults = BuiltInRewriteSettings.defaults();
        var withNullModes = BuiltInRewriteSettings.builder()
            .defaultLimitInjectionValue(25)
            .maxAllowedLimit(50)
            .limitExcessMode(null)
            .qualificationDefaultSchema(" ")
            .qualificationFailureMode(null)
            .identifierNormalizationCaseMode(null)
            .build();

        assertEquals(1000L, defaults.defaultLimitInjectionValue());
        assertEquals(LimitExcessMode.DENY, defaults.limitExcessMode());
        assertEquals(QualificationFailureMode.DENY, defaults.qualificationFailureMode());
        assertEquals(IdentifierNormalizationCaseMode.LOWER, defaults.identifierNormalizationCaseMode());
        assertEquals(LimitExcessMode.DENY, withNullModes.limitExcessMode());
        assertEquals(QualificationFailureMode.DENY, withNullModes.qualificationFailureMode());
        assertEquals(IdentifierNormalizationCaseMode.LOWER, withNullModes.identifierNormalizationCaseMode());
        assertEquals(Integer.valueOf(50), withNullModes.maxAllowedLimit());
        assertNull(withNullModes.qualificationDefaultSchema());
    }

    @Test
    void validates_non_positive_values() {
        assertThrows(IllegalArgumentException.class, () -> BuiltInRewriteSettings.builder()
            .defaultLimitInjectionValue(0)
            .build());
        assertThrows(IllegalArgumentException.class,
            () -> BuiltInRewriteSettings.builder()
                .defaultLimitInjectionValue(10)
                .maxAllowedLimit(0)
                .limitExcessMode(LimitExcessMode.CLAMP)
                .build());
    }
}

