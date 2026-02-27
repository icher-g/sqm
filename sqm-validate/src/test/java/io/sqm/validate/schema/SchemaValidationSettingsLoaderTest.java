package io.sqm.validate.schema;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaValidationSettingsLoaderTest {

    @Test
    void loads_from_json() {
        var json = """
            {
              "accessPolicy": {
                "deniedTables": ["orders", "private.audit_log"],
                "deniedColumns": ["u.secret", "token"],
                "allowedFunctions": ["length", "sum"]
              },
              "limits": {
                "maxJoinCount": 2,
                "maxSelectColumns": 5
              }
            }
            """;

        var settings = SchemaValidationSettingsLoader.fromJson(json);

        assertTrue(settings.accessPolicy().isTableDenied(null, "orders"));
        assertTrue(settings.accessPolicy().isTableDenied("private", "audit_log"));
        assertTrue(settings.accessPolicy().isColumnDenied("u", "secret"));
        assertTrue(settings.accessPolicy().isFunctionAllowed("length"));
        assertEquals(2, settings.limits().maxJoinCount());
        assertEquals(5, settings.limits().maxSelectColumns());
        assertNull(settings.principal());
    }

    @Test
    void loads_from_yaml() {
        var yaml = """
            accessPolicy:
              deniedTables:
                - orders
              deniedColumns:
                - u.secret
              allowedFunctions:
                - length
            limits:
              maxJoinCount: 1
              maxSelectColumns: 3
            """;

        var settings = SchemaValidationSettingsLoader.fromYaml(yaml);

        assertTrue(settings.accessPolicy().isTableDenied(null, "orders"));
        assertTrue(settings.accessPolicy().isColumnDenied("u", "secret"));
        assertTrue(settings.accessPolicy().isFunctionAllowed("length"));
        assertEquals(1, settings.limits().maxJoinCount());
        assertEquals(3, settings.limits().maxSelectColumns());
        assertNull(settings.principal());
    }

    @Test
    void loads_principal_specific_policy_and_principal_context() {
        var json = """
            {
              "principal": "alice",
              "accessPolicy": {
                "principals": [
                  {
                    "name": "alice",
                    "deniedTables": ["orders"],
                    "deniedColumns": ["u.secret"],
                    "allowedFunctions": ["length"]
                  }
                ]
              }
            }
            """;

        var settings = SchemaValidationSettingsLoader.fromJson(json);

        assertEquals("alice", settings.principal());
        assertTrue(settings.accessPolicy().isTableDenied("alice", null, "orders"));
        assertTrue(settings.accessPolicy().isColumnDenied("alice", "u", "secret"));
        assertTrue(settings.accessPolicy().isFunctionAllowed("alice", "length"));
        assertTrue(settings.accessPolicy().isFunctionAllowed("alice", null));
        assertFalse(settings.accessPolicy().isFunctionAllowed("bob", "length"));
    }

    @Test
    void loads_tenant_specific_policy_and_tenant_context() {
        var yaml = """
            tenant: tenant_a
            accessPolicy:
              deniedTables:
                - global_blocked
              tenants:
                - name: tenant_a
                  deniedTables:
                    - users
                  deniedColumns:
                    - u.secret
                  allowedFunctions:
                    - length
                  principals:
                    - name: analyst
                      deniedColumns:
                        - u.email
            """;

        var settings = SchemaValidationSettingsLoader.fromYaml(yaml);

        assertEquals("tenant_a", settings.tenant());
        assertEquals(TenantRequirementMode.OPTIONAL, settings.tenantRequirementMode());
        assertTrue(settings.accessPolicy().isTableDenied(null, null, null, "global_blocked"));
        assertTrue(settings.accessPolicy().isTableDenied("tenant_a", null, null, "users"));
        assertTrue(settings.accessPolicy().isColumnDenied("tenant_a", null, "u", "secret"));
        assertTrue(settings.accessPolicy().isFunctionAllowed("tenant_a", null, "length"));
        assertTrue(settings.accessPolicy().isColumnDenied("tenant_a", "analyst", "u", "email"));
        assertFalse(settings.accessPolicy().isColumnDenied("tenant_a", "viewer", "u", "email"));
        assertFalse(settings.accessPolicy().isTableDenied("tenant_b", null, null, "users"));
    }

    @Test
    void loads_tenant_requirement_mode() {
        var yaml = """
            tenantRequirementMode: required
            """;

        var settings = SchemaValidationSettingsLoader.fromYaml(yaml);

        assertEquals(TenantRequirementMode.REQUIRED, settings.tenantRequirementMode());
    }

    @Test
    void rejects_duplicate_tenant_policy_names() {
        var yaml = """
            accessPolicy:
              tenants:
                - name: tenant_a
                  deniedTables:
                    - users
                - name: tenant_a
                  deniedTables:
                    - orders
            """;

        var ex = assertThrows(IllegalArgumentException.class,
            () -> SchemaValidationSettingsLoader.fromYaml(yaml));
        assertTrue(ex.getMessage().contains("invalid"));
        assertTrue(ex.getMessage().contains("tenant access policy already defined"));
    }

    @Test
    void rejects_blank_tenant_policy_name() {
        var yaml = """
            accessPolicy:
              tenants:
                - name: " "
                  deniedTables:
                    - users
            """;

        var ex = assertThrows(IllegalArgumentException.class,
            () -> SchemaValidationSettingsLoader.fromYaml(yaml));
        assertTrue(ex.getMessage().contains("invalid"));
        assertTrue(ex.getMessage().contains("tenant name must not be blank"));
    }

    @Test
    void rejects_unknown_property() {
        var json = """
            {
              "limits": {"maxJoinCount": 1},
              "unknown": true
            }
            """;

        var ex = assertThrows(IllegalArgumentException.class,
            () -> SchemaValidationSettingsLoader.fromJson(json));
        assertTrue(ex.getMessage().contains("invalid"));
        assertTrue(ex.getMessage().contains("unknown"));
    }

    @Test
    void rejects_invalid_limit_values() {
        var yaml = """
            limits:
              maxJoinCount: -1
            """;

        var ex = assertThrows(IllegalArgumentException.class,
            () -> SchemaValidationSettingsLoader.fromYaml(yaml));
        assertTrue(ex.getMessage().contains("maxJoinCount"));
    }

    @Test
    void rejects_malformed_yaml() {
        var ex = assertThrows(IllegalArgumentException.class,
            () -> SchemaValidationSettingsLoader.fromYaml("limits: ["));
        assertTrue(ex.getMessage().contains("invalid"));
    }
}
