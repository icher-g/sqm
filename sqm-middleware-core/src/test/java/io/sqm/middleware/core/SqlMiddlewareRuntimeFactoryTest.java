package io.sqm.middleware.core;

import io.sqm.control.ConfigKeys;
import io.sqm.middleware.api.AnalyzeRequest;
import io.sqm.middleware.api.ExecutionContextDto;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Map;

import static io.sqm.middleware.api.DecisionKindDto.DENY;
import static io.sqm.middleware.api.ReasonCodeDto.DENY_MAX_SELECT_COLUMNS;
import static io.sqm.middleware.api.ReasonCodeDto.DENY_TABLE;
import static io.sqm.middleware.api.ReasonCodeDto.DENY_TENANT_REQUIRED;
import static org.junit.jupiter.api.Assertions.*;

class SqlMiddlewareRuntimeFactoryTest {

    @Test
    void creates_service_with_default_manual_schema_when_no_properties_set() {
        withProperty(ConfigKeys.SCHEMA_SOURCE.property(), null, () -> {
            var service = SqlMiddlewareRuntimeFactory.createFromEnvironment();
            assertNotNull(service);
        });
    }

    @Test
    void throws_for_unknown_schema_source() {
        withProperty(ConfigKeys.SCHEMA_SOURCE.property(), "unknown", () ->
            assertThrows(IllegalArgumentException.class, SqlMiddlewareRuntimeFactory::createFromEnvironment)
        );
    }

    @Test
    void supports_disabling_rewrite_pipeline_from_property() {
        withProperty(ConfigKeys.REWRITE_ENABLED.property(), "false", () ->
            assertDoesNotThrow(SqlMiddlewareRuntimeFactory::createFromEnvironment)
        );
    }

    @Test
    void applies_validation_and_guardrail_properties_without_errors() {
        withProperty(ConfigKeys.VALIDATION_MAX_JOIN_COUNT.property(), "3", () ->
            withProperty(ConfigKeys.VALIDATION_MAX_SELECT_COLUMNS.property(), "20", () ->
                withProperty(ConfigKeys.GUARDRAILS_MAX_SQL_LENGTH.property(), "5000", () ->
                    withProperty(ConfigKeys.GUARDRAILS_TIMEOUT_MILLIS.property(), "2000", () ->
                        withProperty(ConfigKeys.GUARDRAILS_MAX_ROWS.property(), "100", () ->
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
            ConfigKeys.SCHEMA_SOURCE.property(), "json",
            ConfigKeys.SCHEMA_JSON_PATH.property(), ""
        ), () -> assertThrows(IllegalArgumentException.class, SqlMiddlewareRuntimeFactory::createFromEnvironment));
    }

    @Test
    void throws_when_json_source_path_does_not_exist() {
        withProperties(Map.of(
            ConfigKeys.SCHEMA_SOURCE.property(), "json",
            ConfigKeys.SCHEMA_JSON_PATH.property(), "./does-not-exist-schema.json"
        ), () -> assertThrows(IllegalStateException.class, SqlMiddlewareRuntimeFactory::createFromEnvironment));
    }

    @Test
    void throws_when_manual_default_json_path_does_not_exist() {
        withProperties(Map.of(
            ConfigKeys.SCHEMA_SOURCE.property(), "manual",
            ConfigKeys.SCHEMA_DEFAULT_JSON_PATH.property(), "./missing-default-schema.json"
        ), () -> assertThrows(IllegalStateException.class, SqlMiddlewareRuntimeFactory::createFromEnvironment));
    }

    @Test
    void throws_when_jdbc_source_url_is_missing() {
        withProperties(Map.of(
            ConfigKeys.SCHEMA_SOURCE.property(), "jdbc",
            ConfigKeys.JDBC_URL.property(), ""
        ), () -> assertThrows(IllegalArgumentException.class, SqlMiddlewareRuntimeFactory::createFromEnvironment));
    }

    @Test
    void throws_when_jdbc_driver_class_is_invalid() {
        withProperties(Map.of(
            ConfigKeys.SCHEMA_SOURCE.property(), "jdbc",
            ConfigKeys.JDBC_URL.property(), "jdbc:h2:mem:test",
            ConfigKeys.JDBC_DRIVER.property(), "com.example.MissingDriver"
        ), () -> assertThrows(IllegalArgumentException.class, SqlMiddlewareRuntimeFactory::createFromEnvironment));
    }

    @Test
    void throws_when_rewrite_rule_name_is_invalid() {
        withProperties(Map.of(
            ConfigKeys.SCHEMA_SOURCE.property(), "manual",
            ConfigKeys.REWRITE_RULES.property(), "not_a_rule"
        ), () -> assertThrows(IllegalArgumentException.class, SqlMiddlewareRuntimeFactory::createFromEnvironment));
    }

    @Test
    void throws_when_rewrite_enum_setting_is_invalid() {
        withProperties(Map.of(
            ConfigKeys.SCHEMA_SOURCE.property(), "manual",
            ConfigKeys.REWRITE_LIMIT_EXCESS_MODE.property(), "not_a_mode"
        ), () -> assertThrows(IllegalArgumentException.class, SqlMiddlewareRuntimeFactory::createFromEnvironment));
    }

    @Test
    void throws_when_identifier_case_mode_is_invalid() {
        withProperties(Map.of(
            ConfigKeys.SCHEMA_SOURCE.property(), "manual",
            ConfigKeys.REWRITE_IDENTIFIER_NORMALIZATION_CASE_MODE.property(), "not_a_case_mode"
        ), () -> assertThrows(IllegalArgumentException.class, SqlMiddlewareRuntimeFactory::createFromEnvironment));
    }

    @Test
    void applies_rewrite_and_guardrail_customizations_when_valid_values_present() {
        withProperties(Map.of(
            ConfigKeys.SCHEMA_SOURCE.property(), "manual",
            ConfigKeys.REWRITE_RULES.property(), "LIMIT_INJECTION,CANONICALIZATION",
            ConfigKeys.REWRITE_DEFAULT_LIMIT_INJECTION_VALUE.property(), "100",
            ConfigKeys.REWRITE_MAX_ALLOWED_LIMIT.property(), "1000",
            ConfigKeys.REWRITE_LIMIT_EXCESS_MODE.property(), "DENY",
            ConfigKeys.REWRITE_QUALIFICATION_DEFAULT_SCHEMA.property(), "public",
            ConfigKeys.REWRITE_QUALIFICATION_FAILURE_MODE.property(), "DENY",
            ConfigKeys.REWRITE_IDENTIFIER_NORMALIZATION_CASE_MODE.property(), "LOWER",
            ConfigKeys.GUARDRAILS_EXPLAIN_DRY_RUN.property(), "true"
        ), () -> assertDoesNotThrow(SqlMiddlewareRuntimeFactory::createFromEnvironment));
    }

    @Test
    void merges_validation_settings_config_with_limits_in_runtime_factory() {
        withProperties(Map.of(
            ConfigKeys.SCHEMA_SOURCE.property(), "manual",
            ConfigKeys.VALIDATION_SETTINGS_YAML.property(), """
                accessPolicy:
                  deniedTables:
                    - users
                """,
            ConfigKeys.VALIDATION_MAX_SELECT_COLUMNS.property(), "1"
        ), () -> {
            var service = SqlMiddlewareRuntimeFactory.createFromEnvironment();

            var deniedTable = service.analyze(
                new AnalyzeRequest("select id from users", new ExecutionContextDto("postgresql", null, null, null, null))
            );
            assertEquals(DENY, deniedTable.kind());
            assertEquals(DENY_TABLE, deniedTable.reasonCode());

            var deniedLimit = service.analyze(
                new AnalyzeRequest("select 1, 2", new ExecutionContextDto("postgresql", null, null, null, null))
            );
            assertEquals(DENY, deniedLimit.kind());
            assertEquals(DENY_MAX_SELECT_COLUMNS, deniedLimit.reasonCode());
        });
    }

    @Test
    void applies_principal_aware_access_policy_from_settings_config() {
        withProperties(Map.of(
            ConfigKeys.SCHEMA_SOURCE.property(), "manual",
            ConfigKeys.VALIDATION_SETTINGS_YAML.property(), """
                accessPolicy:
                  principals:
                    - name: alice
                      deniedTables:
                        - users
                """
        ), () -> {
            var service = SqlMiddlewareRuntimeFactory.createFromEnvironment();

            var deniedForAlice = service.analyze(
                new AnalyzeRequest("select id from users", new ExecutionContextDto("postgresql", "alice", null, null, null))
            );
            assertEquals(DENY, deniedForAlice.kind());
            assertEquals(DENY_TABLE, deniedForAlice.reasonCode());

            var allowedForBob = service.analyze(
                new AnalyzeRequest("select id from users", new ExecutionContextDto("postgresql", "bob", null, null, null))
            );
            assertNotSame(DENY, allowedForBob.kind());
        });
    }

    @Test
    void applies_tenant_aware_access_policy_from_settings_config() {
        withProperties(Map.of(
            ConfigKeys.SCHEMA_SOURCE.property(), "manual",
            ConfigKeys.VALIDATION_SETTINGS_YAML.property(), """
                accessPolicy:
                  tenants:
                    - name: tenant_a
                      deniedTables:
                        - users
                """
        ), () -> {
            var service = SqlMiddlewareRuntimeFactory.createFromEnvironment();

            var deniedForTenantA = service.analyze(
                new AnalyzeRequest("select id from users", new ExecutionContextDto("postgresql", null, "tenant_a", null, null))
            );
            assertEquals(DENY, deniedForTenantA.kind());
            assertEquals(DENY_TABLE, deniedForTenantA.reasonCode());

            var allowedForTenantB = service.analyze(
                new AnalyzeRequest("select id from users", new ExecutionContextDto("postgresql", null, "tenant_b", null, null))
            );
            assertNotSame(DENY, allowedForTenantB.kind());
        });
    }

    @Test
    void applies_required_tenant_mode_from_runtime_property() {
        withProperties(Map.of(
            ConfigKeys.SCHEMA_SOURCE.property(), "manual",
            ConfigKeys.VALIDATION_TENANT_REQUIREMENT_MODE.property(), "required"
        ), () -> {
            var service = SqlMiddlewareRuntimeFactory.createFromEnvironment();

            var denied = service.analyze(
                new AnalyzeRequest("select 1", new ExecutionContextDto("postgresql", null, null, null, null))
            );
            assertEquals(DENY, denied.kind());
            assertEquals(DENY_TENANT_REQUIRED, denied.reasonCode());
        });
    }

    @Test
    void throws_when_tenant_requirement_mode_is_invalid() {
        withProperties(Map.of(
            ConfigKeys.SCHEMA_SOURCE.property(), "manual",
            ConfigKeys.VALIDATION_TENANT_REQUIREMENT_MODE.property(), "invalid_mode"
        ), () -> assertThrows(IllegalArgumentException.class, SqlMiddlewareRuntimeFactory::createFromEnvironment));
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

