package io.sqm.control;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TenantRewriteTablePolicyTest {

    @Test
    void defaults_mode_to_required_and_trims_column() {
        var policy = TenantRewriteTablePolicy.of(" tenant_id ", null);
        assertEquals("tenant_id", policy.tenantColumn());
        assertEquals(TenantRewriteTableMode.REQUIRED, policy.mode());
    }

    @Test
    void required_factory_sets_required_mode() {
        var policy = TenantRewriteTablePolicy.required("tenant_col");
        assertEquals("tenant_col", policy.tenantColumn());
        assertEquals(TenantRewriteTableMode.REQUIRED, policy.mode());
    }

    @Test
    void rejects_blank_column() {
        assertThrows(IllegalArgumentException.class, () -> TenantRewriteTablePolicy.of(" ", TenantRewriteTableMode.OPTIONAL));
    }
}

