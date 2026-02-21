package io.sqm.validate.schema;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
