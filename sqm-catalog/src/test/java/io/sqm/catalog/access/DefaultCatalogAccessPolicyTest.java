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
}

