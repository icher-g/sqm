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
            .tenantFallbackMode(null)
            .tenantAmbiguityMode(null)
            .build();

        assertEquals(1000L, defaults.defaultLimitInjectionValue());
        assertEquals(LimitExcessMode.DENY, defaults.limitExcessMode());
        assertEquals(QualificationFailureMode.DENY, defaults.qualificationFailureMode());
        assertEquals(IdentifierNormalizationCaseMode.LOWER, defaults.identifierNormalizationCaseMode());
        assertEquals(TenantRewriteFallbackMode.DENY, defaults.tenantFallbackMode());
        assertEquals(TenantRewriteAmbiguityMode.DENY, defaults.tenantAmbiguityMode());
        assertTrue(defaults.tenantTablePolicies().isEmpty());
        assertEquals(LimitExcessMode.DENY, withNullModes.limitExcessMode());
        assertEquals(QualificationFailureMode.DENY, withNullModes.qualificationFailureMode());
        assertEquals(IdentifierNormalizationCaseMode.LOWER, withNullModes.identifierNormalizationCaseMode());
        assertEquals(TenantRewriteFallbackMode.DENY, withNullModes.tenantFallbackMode());
        assertEquals(TenantRewriteAmbiguityMode.DENY, withNullModes.tenantAmbiguityMode());
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

    @Test
    void normalizes_and_validates_tenant_table_policies() {
        var settings = BuiltInRewriteSettings.builder()
            .tenantTablePolicy(" Public.Users ", TenantRewriteTablePolicy.of(" tenant_id ", TenantRewriteTableMode.OPTIONAL))
            .build();

        var policy = settings.tenantTablePolicies().get("public.users");
        assertNotNull(policy);
        assertEquals("tenant_id", policy.tenantColumn());
        assertEquals(TenantRewriteTableMode.OPTIONAL, policy.mode());

        assertThrows(UnsupportedOperationException.class, () -> settings.tenantTablePolicies().put(
            "x.y",
            TenantRewriteTablePolicy.required("tenant_id")
        ));
        assertThrows(IllegalArgumentException.class, () -> BuiltInRewriteSettings.builder()
            .tenantTablePolicy("users", TenantRewriteTablePolicy.required("tenant_id"))
            .build());
        assertThrows(IllegalArgumentException.class, () -> BuiltInRewriteSettings.builder()
            .tenantTablePolicy("public.users", TenantRewriteTablePolicy.required(" "))
            .build());
    }
}

