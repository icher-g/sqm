package io.sqm.control;

import io.sqm.core.transform.IdentifierNormalizationCaseMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BuiltInRewriteSettingsTest {

    @Test
    void defaults_and_null_mode_fallback_to_deny() {
        var defaults = BuiltInRewriteSettings.defaults();
        var withNullModes = new BuiltInRewriteSettings(25, 50, null, " ", null, null);

        assertEquals(1000L, defaults.defaultLimitInjectionValue());
        assertEquals(BuiltInRewriteSettings.LimitExcessMode.DENY, defaults.limitExcessMode());
        assertEquals(BuiltInRewriteSettings.QualificationFailureMode.DENY, defaults.qualificationFailureMode());
        assertEquals(IdentifierNormalizationCaseMode.LOWER, defaults.identifierNormalizationCaseMode());
        assertEquals(BuiltInRewriteSettings.LimitExcessMode.DENY, withNullModes.limitExcessMode());
        assertEquals(BuiltInRewriteSettings.QualificationFailureMode.DENY, withNullModes.qualificationFailureMode());
        assertEquals(IdentifierNormalizationCaseMode.LOWER, withNullModes.identifierNormalizationCaseMode());
        assertEquals(Integer.valueOf(50), withNullModes.maxAllowedLimit());
        assertNull(withNullModes.qualificationDefaultSchema());
    }

    @Test
    void validates_non_positive_values() {
        assertThrows(IllegalArgumentException.class, () -> new BuiltInRewriteSettings(0));
        assertThrows(IllegalArgumentException.class,
            () -> new BuiltInRewriteSettings(10, 0, BuiltInRewriteSettings.LimitExcessMode.CLAMP));
    }
}

