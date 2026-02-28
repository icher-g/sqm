package io.sqm.middleware.core;

import io.sqm.control.ConfigKeys;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class SchemaBootstrapLoaderTest {

    @Test
    void constructor_rejects_null_reader() {
        assertThrows(NullPointerException.class, () -> new SchemaBootstrapLoader(null));
    }

    @Test
    void bootstrap_wraps_error_in_fail_fast_mode() {
        var loader = new SchemaBootstrapLoader((key, defaultValue) -> defaultValue);
        var ex = assertThrows(IllegalArgumentException.class, () -> loader.bootstrap("unknown", true));
        assertTrue(ex.getMessage().contains("Schema bootstrap failed [source=unknown]"));
        assertNotNull(ex.getCause());
    }

    @Test
    void bootstrap_returns_degraded_when_fail_fast_disabled() {
        var loader = new SchemaBootstrapLoader((key, defaultValue) -> defaultValue);
        var result = loader.bootstrap("unknown", false);
        assertFalse(result.ready());
        assertNull(result.schemaLoad());
        assertNotNull(result.degradedMessage());
        assertTrue(result.degradedMessage().contains("Unsupported schema source"));
    }

    @Test
    void bootstrap_loads_manual_schema_from_explicit_json_path() throws Exception {
        var path = copyBundledDefaultSchemaToTempPath();
        try {
            var values = new HashMap<ConfigKeys.Key, String>();
            values.put(ConfigKeys.SCHEMA_DEFAULT_JSON_PATH, path.toString());
            var loader = new SchemaBootstrapLoader((key, defaultValue) -> values.getOrDefault(key, defaultValue));

            var result = loader.bootstrap("manual", true);
            assertTrue(result.ready());
            assertNotNull(result.schemaLoad());
            assertTrue(result.schemaLoad().description().contains(path.toString()));
        } finally {
            Files.deleteIfExists(path);
        }
    }

    @Test
    void bootstrap_result_and_schema_load_result_validate_invariants() {
        assertThrows(IllegalArgumentException.class, () -> new SchemaBootstrapLoader.BootstrapResult("", null, "x"));
        assertThrows(IllegalArgumentException.class, () -> new SchemaBootstrapLoader.BootstrapResult("json", null, ""));
        assertThrows(IllegalArgumentException.class, () -> new SchemaBootstrapLoader.BootstrapResult("json", null, null));
        var schemaLoad = new SchemaBootstrapLoader.SchemaLoadResult(io.sqm.catalog.model.CatalogSchema.of(), "ok");
        assertThrows(IllegalArgumentException.class, () -> new SchemaBootstrapLoader.BootstrapResult("json", schemaLoad, "x"));
        assertThrows(
            IllegalArgumentException.class,
            () -> new SchemaBootstrapLoader.SchemaLoadResult(io.sqm.catalog.model.CatalogSchema.of(), "")
        );
    }

    @Test
    void driver_manager_data_source_rejects_null_url() {
        assertThrows(NullPointerException.class, () -> new SchemaBootstrapLoader.DriverManagerDataSource(null, "", ""));
    }

    private Path copyBundledDefaultSchemaToTempPath() throws Exception {
        var target = Files.createTempFile("sqm-schema-bootstrap-loader-test-schema", ".json");
        try (InputStream in = SqlMiddlewareRuntimeFactory.class.getResourceAsStream("/io/sqm/middleware/core/default-schema.json")) {
            if (in == null) {
                throw new IllegalStateException("Missing bundled schema resource for test");
            }
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }
}
