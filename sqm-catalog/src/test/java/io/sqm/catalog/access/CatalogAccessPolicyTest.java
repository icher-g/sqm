package io.sqm.catalog.access;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogAccessPolicyTest {

    @Test
    void default_overloads_delegate_to_principal_aware_methods() {
        var policy = new CatalogAccessPolicy() {
            @Override
            public boolean isTableDenied(String principal, String schemaName, String tableName) {
                return "p1".equals(principal) && "public".equals(schemaName) && "users".equals(tableName);
            }

            @Override
            public boolean isColumnDenied(String principal, String sourceName, String columnName) {
                return "p1".equals(principal) && "u".equals(sourceName) && "secret".equals(columnName);
            }

            @Override
            public boolean isFunctionAllowed(String principal, String functionName) {
                return "p1".equals(principal) && "length".equals(functionName);
            }
        };

        assertTrue(policy.isTableDenied("tenant-a", "p1", "public", "users"));
        assertTrue(policy.isColumnDenied("tenant-a", "p1", "u", "secret"));
        assertTrue(policy.isFunctionAllowed("tenant-a", "p1", "length"));

        assertTrue(policy.isTableDenied("p1", "public", "users"));
        assertTrue(policy.isColumnDenied("p1", "u", "secret"));
        assertTrue(policy.isFunctionAllowed("p1", "length"));

        assertFalse(policy.isTableDenied("public", "users"));
        assertFalse(policy.isColumnDenied("u", "secret"));
        assertFalse(policy.isFunctionAllowed("length"));
    }
}
