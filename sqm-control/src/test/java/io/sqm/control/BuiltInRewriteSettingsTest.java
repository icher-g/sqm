package io.sqm.control;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BuiltInRewriteSettingsTest {

    @Test
    void defaults_and_null_mode_fallback_to_deny() {
        var defaults = BuiltInRewriteSettings.defaults();
        var withNullMode = new BuiltInRewriteSettings(25, 50, null);

        assertEquals(1000L, defaults.defaultLimitInjectionValue());
        assertEquals(BuiltInRewriteSettings.LimitExcessMode.DENY, defaults.limitExcessMode());
        assertEquals(BuiltInRewriteSettings.LimitExcessMode.DENY, withNullMode.limitExcessMode());
        assertEquals(Integer.valueOf(50), withNullMode.maxAllowedLimit());
    }

    @Test
    void validates_non_positive_values() {
        assertThrows(IllegalArgumentException.class, () -> new BuiltInRewriteSettings(0));
        assertThrows(IllegalArgumentException.class,
            () -> new BuiltInRewriteSettings(10, 0, BuiltInRewriteSettings.LimitExcessMode.CLAMP));
    }
}

