package io.sqm.catalog.access;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultCatalogAccessPolicyTest {
    @Test
    void allow_all_policy_has_no_restrictions() {
        CatalogAccessPolicy policy = DefaultCatalogAccessPolicy.allowAll();
        assertFalse(policy.isTableDenied("alice", "public", "users"));
        assertFalse(policy.isColumnDenied("alice", "u", "secret"));
        assertTrue(policy.isFunctionAllowed("alice", "lower"));
    }

    @Test
    void global_rules_are_case_insensitive() {
        var policy = DefaultCatalogAccessPolicy.builder()
            .denyTable("private.audit_log")
            .denyColumn("u.secret")
            .allowFunction("length")
            .build();

        assertTrue(policy.isTableDenied("PUBLIC", "PRIVATE", "AUDIT_LOG"));
        assertTrue(policy.isColumnDenied("PUBLIC", "U", "SECRET"));
        assertTrue(policy.isFunctionAllowed("PUBLIC", "LENGTH"));
        assertFalse(policy.isFunctionAllowed("PUBLIC", "lower"));
    }

    @Test
    void principal_rules_are_applied_to_matching_principal_only() {
        var policy = DefaultCatalogAccessPolicy.builder()
            .denyTableForPrincipal("alice", "orders")
            .denyColumnForPrincipal("alice", "u.secret")
            .allowFunctionForPrincipal("alice", "length")
            .build();

        assertTrue(policy.isTableDenied("alice", null, "orders"));
        assertFalse(policy.isTableDenied("bob", null, "orders"));

        assertTrue(policy.isColumnDenied("alice", "u", "secret"));
        assertFalse(policy.isColumnDenied("bob", "u", "secret"));

        assertTrue(policy.isFunctionAllowed("alice", "length"));
        assertFalse(policy.isFunctionAllowed("alice", "lower"));
        assertFalse(policy.isFunctionAllowed("bob", "length"));
    }

    @Test
    void tenant_and_tenant_principal_rules_are_applied_for_matching_context_only() {
        var policy = DefaultCatalogAccessPolicy.builder()
            .denyTableForTenant("tenant_a", "orders")
            .denyColumnForTenant("tenant_a", "u.secret")
            .allowFunctionForTenant("tenant_a", "length")
            .denyTableForTenantPrincipal("tenant_a", "alice", "users")
            .denyColumnForTenantPrincipal("tenant_a", "alice", "u.email")
            .allowFunctionForTenantPrincipal("tenant_a", "alice", "lower")
            .build();

        assertTrue(policy.isTableDenied("tenant_a", "bob", null, "orders"));
        assertFalse(policy.isTableDenied("tenant_b", "bob", null, "orders"));

        assertTrue(policy.isColumnDenied("tenant_a", "bob", "u", "secret"));
        assertFalse(policy.isColumnDenied("tenant_b", "bob", "u", "secret"));

        assertTrue(policy.isFunctionAllowed("tenant_a", "bob", "length"));
        assertFalse(policy.isFunctionAllowed("tenant_b", "bob", "length"));

        assertTrue(policy.isTableDenied("tenant_a", "alice", null, "users"));
        assertFalse(policy.isTableDenied("tenant_a", "bob", null, "users"));

        assertTrue(policy.isColumnDenied("tenant_a", "alice", "u", "email"));
        assertFalse(policy.isColumnDenied("tenant_a", "bob", "u", "email"));

        assertTrue(policy.isFunctionAllowed("tenant_a", "alice", "lower"));
        assertFalse(policy.isFunctionAllowed("tenant_a", "bob", "lower"));
    }
}

