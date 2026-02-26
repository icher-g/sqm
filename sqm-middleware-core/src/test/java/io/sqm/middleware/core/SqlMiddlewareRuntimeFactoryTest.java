package io.sqm.middleware.core;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlMiddlewareRuntimeFactoryTest {

    @Test
    void creates_service_with_default_manual_schema_when_no_properties_set() {
        withProperty("sqm.middleware.schema.source", null, () -> {
            var service = SqlMiddlewareRuntimeFactory.createFromEnvironment();
            assertNotNull(service);
        });
    }

    @Test
    void throws_for_unknown_schema_source() {
        withProperty("sqm.middleware.schema.source", "unknown", () ->
            assertThrows(IllegalArgumentException.class, SqlMiddlewareRuntimeFactory::createFromEnvironment)
        );
    }

    @Test
    void supports_disabling_rewrite_pipeline_from_property() {
        withProperty("sqm.middleware.rewrite.enabled", "false", () ->
            assertDoesNotThrow(SqlMiddlewareRuntimeFactory::createFromEnvironment)
        );
    }

    @Test
    void applies_validation_and_guardrail_properties_without_errors() {
        withProperty("sqm.middleware.validation.maxJoinCount", "3", () ->
            withProperty("sqm.middleware.validation.maxSelectColumns", "20", () ->
                withProperty("sqm.middleware.guardrails.maxSqlLength", "5000", () ->
                    withProperty("sqm.middleware.guardrails.timeoutMillis", "2000", () ->
                        withProperty("sqm.middleware.guardrails.maxRows", "100", () ->
                            assertDoesNotThrow(SqlMiddlewareRuntimeFactory::createFromEnvironment)
                        )
                    )
                )
            )
        );
    }

    @Test
    void throws_when_json_source_path_is_missing() {
        withProperties(Map.of(
            "sqm.middleware.schema.source", "json",
            "sqm.middleware.schema.json.path", ""
        ), () -> assertThrows(IllegalArgumentException.class, SqlMiddlewareRuntimeFactory::createFromEnvironment));
    }

    @Test
    void throws_when_json_source_path_does_not_exist() {
        withProperties(Map.of(
            "sqm.middleware.schema.source", "json",
            "sqm.middleware.schema.json.path", "./does-not-exist-schema.json"
        ), () -> assertThrows(IllegalStateException.class, SqlMiddlewareRuntimeFactory::createFromEnvironment));
    }

    @Test
    void throws_when_manual_default_json_path_does_not_exist() {
        withProperties(Map.of(
            "sqm.middleware.schema.source", "manual",
            "sqm.middleware.schema.defaultJson.path", "./missing-default-schema.json"
        ), () -> assertThrows(IllegalStateException.class, SqlMiddlewareRuntimeFactory::createFromEnvironment));
    }

    @Test
    void throws_when_jdbc_source_url_is_missing() {
        withProperties(Map.of(
            "sqm.middleware.schema.source", "jdbc",
            "sqm.middleware.jdbc.url", ""
        ), () -> assertThrows(IllegalArgumentException.class, SqlMiddlewareRuntimeFactory::createFromEnvironment));
    }

    @Test
    void throws_when_jdbc_driver_class_is_invalid() {
        withProperties(Map.of(
            "sqm.middleware.schema.source", "jdbc",
            "sqm.middleware.jdbc.url", "jdbc:h2:mem:test",
            "sqm.middleware.jdbc.driver", "com.example.MissingDriver"
        ), () -> assertThrows(IllegalArgumentException.class, SqlMiddlewareRuntimeFactory::createFromEnvironment));
    }

    @Test
    void throws_when_rewrite_rule_name_is_invalid() {
        withProperties(Map.of(
            "sqm.middleware.schema.source", "manual",
            "sqm.middleware.rewrite.rules", "not_a_rule"
        ), () -> assertThrows(IllegalArgumentException.class, SqlMiddlewareRuntimeFactory::createFromEnvironment));
    }

    @Test
    void throws_when_rewrite_enum_setting_is_invalid() {
        withProperties(Map.of(
            "sqm.middleware.schema.source", "manual",
            "sqm.middleware.rewrite.limitExcessMode", "not_a_mode"
        ), () -> assertThrows(IllegalArgumentException.class, SqlMiddlewareRuntimeFactory::createFromEnvironment));
    }

    @Test
    void throws_when_identifier_case_mode_is_invalid() {
        withProperties(Map.of(
            "sqm.middleware.schema.source", "manual",
            "sqm.middleware.rewrite.identifierNormalizationCaseMode", "not_a_case_mode"
        ), () -> assertThrows(IllegalArgumentException.class, SqlMiddlewareRuntimeFactory::createFromEnvironment));
    }

    @Test
    void applies_rewrite_and_guardrail_customizations_when_valid_values_present() {
        withProperties(Map.of(
            "sqm.middleware.schema.source", "manual",
            "sqm.middleware.rewrite.rules", "LIMIT_INJECTION,CANONICALIZATION",
            "sqm.middleware.rewrite.defaultLimitInjectionValue", "100",
            "sqm.middleware.rewrite.maxAllowedLimit", "1000",
            "sqm.middleware.rewrite.limitExcessMode", "DENY",
            "sqm.middleware.rewrite.qualificationDefaultSchema", "public",
            "sqm.middleware.rewrite.qualificationFailureMode", "DENY",
            "sqm.middleware.rewrite.identifierNormalizationCaseMode", "LOWER",
            "sqm.middleware.guardrails.explainDryRun", "true"
        ), () -> assertDoesNotThrow(SqlMiddlewareRuntimeFactory::createFromEnvironment));
    }

    @Test
    void driver_manager_data_source_support_methods_are_covered() throws Exception {
        Class<?> dsClass = Class.forName("io.sqm.middleware.core.SqlMiddlewareRuntimeFactory$DriverManagerDataSource");
        var constructor = dsClass.getDeclaredConstructor(String.class, String.class, String.class);
        constructor.setAccessible(true);
        var dataSource = (DataSource) constructor.newInstance("jdbc:invalid:test", "", "");

        assertFalse(dataSource.isWrapperFor(Object.class));
        assertThrows(SQLFeatureNotSupportedException.class, () -> dataSource.unwrap(Object.class));
        assertNotNull(dataSource.getParentLogger());

        dataSource.setLoginTimeout(1);
        assertTrue(dataSource.getLoginTimeout() >= 0);

        var writer = new PrintWriter(new StringWriter());
        dataSource.setLogWriter(writer);
        assertNotNull(dataSource.getLogWriter());

        assertThrows(SQLException.class, dataSource::getConnection);
        assertThrows(SQLException.class, () -> dataSource.getConnection("u", "p"));
    }

    private void withProperty(String key, String value, Runnable runnable) {
        var previous = System.getProperty(key);
        try {
            if (value == null) {
                System.clearProperty(key);
            }
            else {
                System.setProperty(key, value);
            }
            runnable.run();
        }
        finally {
            if (previous == null) {
                System.clearProperty(key);
            }
            else {
                System.setProperty(key, previous);
            }
        }
    }

    private void withProperties(Map<String, String> properties, Runnable runnable) {
        var previous = new java.util.HashMap<String, String>();
        properties.keySet().forEach(key -> previous.put(key, System.getProperty(key)));
        try {
            properties.forEach((key, value) -> {
                if (value == null) {
                    System.clearProperty(key);
                }
                else {
                    System.setProperty(key, value);
                }
            });
            runnable.run();
        }
        finally {
            previous.forEach((key, value) -> {
                if (value == null) {
                    System.clearProperty(key);
                }
                else {
                    System.setProperty(key, value);
                }
            });
        }
    }
}
