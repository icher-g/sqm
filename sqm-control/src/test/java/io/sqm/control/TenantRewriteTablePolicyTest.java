package io.sqm.control;

import io.sqm.control.audit.*;
import io.sqm.control.config.*;
import io.sqm.control.decision.*;
import io.sqm.control.execution.*;
import io.sqm.control.pipeline.*;
import io.sqm.control.rewrite.*;
import io.sqm.control.service.*;

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



