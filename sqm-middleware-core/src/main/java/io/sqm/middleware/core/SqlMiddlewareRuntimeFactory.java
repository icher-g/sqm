package io.sqm.middleware.core;

import io.sqm.catalog.jdbc.JdbcSchemaProvider;
import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.snapshot.JsonSchemaProvider;
import io.sqm.control.*;
import io.sqm.middleware.api.SqlMiddlewareService;
import io.sqm.validate.schema.SchemaValidationLimits;
import io.sqm.validate.schema.SchemaValidationSettings;
import io.sqm.validate.schema.SchemaValidationSettingsLoader;
import io.sqm.validate.schema.TenantRequirementMode;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.*;
import java.util.logging.Logger;

import static io.sqm.core.transform.IdentifierNormalizationCaseMode.valueOf;

/**
 * Shared runtime factory for building middleware service instances from external configuration.
 */
public final class SqlMiddlewareRuntimeFactory {

    private static final String DEFAULT_SCHEMA_RESOURCE = "/io/sqm/middleware/core/default-schema.json";

    private SqlMiddlewareRuntimeFactory() {
    }

    /**
     * Creates middleware service from system properties/environment.
     *
     * @return configured middleware service
     */
    public static SqlMiddlewareService createFromEnvironment() {
        var schema = loadSchema();

        var builder = SqlDecisionServiceConfig.builder(schema);

        applyValidationSettings(builder);
        applyGuardrails(builder);

        if (readBoolean(ConfigKeys.REWRITE_ENABLED, true)) {
            applyRewriteCustomizations(builder);
            return SqlMiddlewareServices.create(builder.buildValidationAndRewriteConfig());
        }

        return SqlMiddlewareServices.create(builder.buildValidationConfig());
    }

    private static CatalogSchema loadSchema() {
        var source = readString(ConfigKeys.SCHEMA_SOURCE, "manual")
            .trim()
            .toLowerCase(Locale.ROOT);

        return switch (source) {
            case "json" -> loadJsonSchema();
            case "jdbc" -> loadJdbcSchema();
            case "manual" -> loadDefaultJsonSchema();
            default -> throw new IllegalArgumentException("Unsupported schema source: " + source + ". Supported: manual,json,jdbc");
        };
    }

    private static CatalogSchema loadDefaultJsonSchema() {
        var defaultPath = readString(
            ConfigKeys.SCHEMA_DEFAULT_JSON_PATH,
            null
        );

        if (defaultPath != null && !defaultPath.isBlank()) {
            return loadJsonSchema(Path.of(defaultPath));
        }

        try (InputStream stream = SqlMiddlewareRuntimeFactory.class.getResourceAsStream(DEFAULT_SCHEMA_RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing bundled default schema resource: " + DEFAULT_SCHEMA_RESOURCE);
            }

            var tempFile = Files.createTempFile("sqm-default-schema", ".json");
            Files.copy(stream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            tempFile.toFile().deleteOnExit();
            return loadJsonSchema(tempFile);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load bundled default schema JSON resource", ex);
        }
    }

    private static CatalogSchema loadJsonSchema() {
        var path = readString(ConfigKeys.SCHEMA_JSON_PATH, null);
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException(
                "JSON schema source requires %s (or %s)".formatted(
                    ConfigKeys.SCHEMA_JSON_PATH.property(),
                    ConfigKeys.SCHEMA_JSON_PATH.env()
                )
            );
        }
        return loadJsonSchema(Path.of(path));
    }

    private static CatalogSchema loadJsonSchema(Path path) {
        try {
            return JsonSchemaProvider.of(path).load();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load schema from JSON path: " + path, ex);
        }
    }

    private static CatalogSchema loadJdbcSchema() {
        var url = required(ConfigKeys.JDBC_URL);
        var user = readString(ConfigKeys.JDBC_USER, "");
        var password = readString(ConfigKeys.JDBC_PASSWORD, "");
        var schemaPattern = readString(ConfigKeys.JDBC_SCHEMA_PATTERN, null);
        var driverClass = readString(ConfigKeys.JDBC_DRIVER, null);

        if (driverClass != null && !driverClass.isBlank()) {
            try {
                Class.forName(driverClass);
            } catch (ClassNotFoundException ex) {
                throw new IllegalArgumentException("JDBC driver class not found: " + driverClass, ex);
            }
        }

        DataSource dataSource = new DriverManagerDataSource(url, user, password);
        try {
            return JdbcSchemaProvider.of(dataSource, schemaPattern).load();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load schema from JDBC metadata", ex);
        }
    }

    private static void applyValidationSettings(SqlDecisionServiceConfig.Builder builder) {
        var baseSettings = readValidationSettingsConfig();
        Integer maxJoinCount = readIntNullable(ConfigKeys.VALIDATION_MAX_JOIN_COUNT);
        Integer maxSelectColumns = readIntNullable(ConfigKeys.VALIDATION_MAX_SELECT_COLUMNS);
        var tenantRequirementMode = readEnumNullable(ConfigKeys.VALIDATION_TENANT_REQUIREMENT_MODE, TenantRequirementMode.class);

        if (maxJoinCount == null && maxSelectColumns == null && tenantRequirementMode == null) {
            if (baseSettings != null) {
                builder.validationSettings(baseSettings);
            }
            return;
        }

        var limitsBuilder = SchemaValidationLimits.builder();
        if (maxJoinCount != null) {
            limitsBuilder.maxJoinCount(maxJoinCount);
        }
        if (maxSelectColumns != null) {
            limitsBuilder.maxSelectColumns(maxSelectColumns);
        }

        var limits = limitsBuilder.build();
        if (baseSettings != null) {
            builder.validationSettings(
                SchemaValidationSettings.builder()
                    .functionCatalog(baseSettings.functionCatalog())
                    .accessPolicy(baseSettings.accessPolicy())
                    .principal(baseSettings.principal())
                    .tenant(baseSettings.tenant())
                    .tenantRequirementMode(tenantRequirementMode == null
                        ? baseSettings.tenantRequirementMode()
                        : tenantRequirementMode)
                    .limits(limits)
                    .addRules(baseSettings.additionalRules())
                    .build()
            );
            return;
        }

        builder.validationSettings(
            SchemaValidationSettings.builder()
                .tenantRequirementMode(tenantRequirementMode == null ? TenantRequirementMode.OPTIONAL : tenantRequirementMode)
                .limits(limits)
                .build()
        );
    }

    private static SchemaValidationSettings readValidationSettingsConfig() {
        var json = readString(ConfigKeys.VALIDATION_SETTINGS_JSON, null);
        if (json != null && !json.isBlank()) {
            return SchemaValidationSettingsLoader.fromJson(json);
        }
        var yaml = readString(ConfigKeys.VALIDATION_SETTINGS_YAML, null);
        if (yaml != null && !yaml.isBlank()) {
            return SchemaValidationSettingsLoader.fromYaml(yaml);
        }
        return null;
    }

    private static void applyGuardrails(SqlDecisionServiceConfig.Builder builder) {
        Integer maxSqlLength = readIntNullable(ConfigKeys.GUARDRAILS_MAX_SQL_LENGTH);
        Long timeoutMillis = readLongNullable(ConfigKeys.GUARDRAILS_TIMEOUT_MILLIS);
        Integer maxRows = readIntNullable(ConfigKeys.GUARDRAILS_MAX_ROWS);
        boolean explainDryRun = readBoolean(ConfigKeys.GUARDRAILS_EXPLAIN_DRY_RUN, false);

        if (maxSqlLength == null && timeoutMillis == null && maxRows == null && !explainDryRun) {
            return;
        }

        builder.guardrails(new RuntimeGuardrails(maxSqlLength, timeoutMillis, maxRows, explainDryRun));
    }

    private static void applyRewriteCustomizations(SqlDecisionServiceConfig.Builder builder) {
        var rules = readRewriteRules();
        if (!rules.isEmpty()) {
            builder.rewriteRules(rules.toArray(BuiltInRewriteRule[]::new));
        }

        var settings = readRewriteSettings();
        if (settings != null) {
            builder.builtInRewriteSettings(settings);
        }
    }

    private static Set<BuiltInRewriteRule> readRewriteRules() {
        var raw = readString(ConfigKeys.REWRITE_RULES, null);
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }

        var values = Arrays.stream(raw.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(v -> BuiltInRewriteRule.valueOf(v.toUpperCase(Locale.ROOT)))
            .collect(java.util.stream.Collectors.toCollection(() -> EnumSet.noneOf(BuiltInRewriteRule.class)));

        return Set.copyOf(values);
    }

    private static BuiltInRewriteSettings readRewriteSettings() {
        boolean configured = false;
        var settingsBuilder = BuiltInRewriteSettings.builder();

        Long defaultLimit = readLongNullable(ConfigKeys.REWRITE_DEFAULT_LIMIT_INJECTION_VALUE);
        if (defaultLimit != null) {
            settingsBuilder.defaultLimitInjectionValue(defaultLimit);
            configured = true;
        }

        Integer maxAllowedLimit = readIntNullable(ConfigKeys.REWRITE_MAX_ALLOWED_LIMIT);
        if (maxAllowedLimit != null) {
            settingsBuilder.maxAllowedLimit(maxAllowedLimit);
            configured = true;
        }

        var limitExcessMode = readEnumNullable(ConfigKeys.REWRITE_LIMIT_EXCESS_MODE, LimitExcessMode.class);
        if (limitExcessMode != null) {
            settingsBuilder.limitExcessMode(limitExcessMode);
            configured = true;
        }

        var qualificationDefaultSchema = readString(ConfigKeys.REWRITE_QUALIFICATION_DEFAULT_SCHEMA, null);
        if (qualificationDefaultSchema != null && !qualificationDefaultSchema.isBlank()) {
            settingsBuilder.qualificationDefaultSchema(qualificationDefaultSchema);
            configured = true;
        }

        var qualificationFailureMode = readEnumNullable(ConfigKeys.REWRITE_QUALIFICATION_FAILURE_MODE, QualificationFailureMode.class);
        if (qualificationFailureMode != null) {
            settingsBuilder.qualificationFailureMode(qualificationFailureMode);
            configured = true;
        }

        var identifierCase = readString(ConfigKeys.REWRITE_IDENTIFIER_NORMALIZATION_CASE_MODE, null);
        if (identifierCase != null && !identifierCase.isBlank()) {
            settingsBuilder.identifierNormalizationCaseMode(valueOf(identifierCase.toUpperCase(Locale.ROOT)));
            configured = true;
        }

        var tenantPolicies = readTenantTablePolicies();
        if (!tenantPolicies.isEmpty()) {
            settingsBuilder.tenantTablePolicies(tenantPolicies);
            configured = true;
        }

        var tenantFallbackMode = readEnumNullable(ConfigKeys.REWRITE_TENANT_FALLBACK_MODE, TenantRewriteFallbackMode.class);
        if (tenantFallbackMode != null) {
            settingsBuilder.tenantFallbackMode(tenantFallbackMode);
            configured = true;
        }

        var tenantAmbiguityMode = readEnumNullable(ConfigKeys.REWRITE_TENANT_AMBIGUITY_MODE, TenantRewriteAmbiguityMode.class);
        if (tenantAmbiguityMode != null) {
            settingsBuilder.tenantAmbiguityMode(tenantAmbiguityMode);
            configured = true;
        }

        return configured ? settingsBuilder.build() : null;
    }

    private static Map<String, TenantRewriteTablePolicy> readTenantTablePolicies() {
        var raw = readString(ConfigKeys.REWRITE_TENANT_TABLE_POLICIES, null);
        if (raw == null || raw.isBlank()) {
            return Map.of();
        }

        var entries = Arrays.stream(raw.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();

        if (entries.isEmpty()) {
            return Map.of();
        }

        var policies = new LinkedHashMap<String, TenantRewriteTablePolicy>(entries.size());
        for (String entry : entries) {
            var parts = entry.split(":");
            if (parts.length < 2 || parts.length > 3) {
                throw new IllegalArgumentException(
                    "Invalid tenant table policy entry '%s'. Expected schema.table:tenant_column[:mode]".formatted(entry)
                );
            }

            var table = parts[0].trim();
            var tenantColumn = parts[1].trim();
            var mode = parts.length == 3
                ? TenantRewriteTableMode.valueOf(parts[2].trim().toUpperCase(Locale.ROOT))
                : TenantRewriteTableMode.REQUIRED;

            policies.put(table, TenantRewriteTablePolicy.of(tenantColumn, mode));
        }
        return Map.copyOf(policies);
    }

    private static String required(ConfigKeys.Key key) {
        var value = readString(key, null);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required configuration: " + key.property() + " / " + key.env());
        }
        return value;
    }

    private static String readString(ConfigKeys.Key key, String defaultValue) {
        var value = System.getProperty(key.property());
        if (value == null || value.isBlank()) {
            value = System.getenv(key.env());
        }
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }

    private static boolean readBoolean(ConfigKeys.Key key, boolean defaultValue) {
        var raw = readString(key, null);
        return raw == null ? defaultValue : Boolean.parseBoolean(raw);
    }

    private static Integer readIntNullable(ConfigKeys.Key key) {
        var raw = readString(key, null);
        return raw == null ? null : Integer.valueOf(raw);
    }

    private static Long readLongNullable(ConfigKeys.Key key) {
        var raw = readString(key, null);
        return raw == null ? null : Long.valueOf(raw);
    }

    private static <E extends Enum<E>> E readEnumNullable(ConfigKeys.Key key, Class<E> enumType) {
        Objects.requireNonNull(enumType, "enumType must not be null");
        var raw = readString(key, null);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return Enum.valueOf(enumType, raw.trim().toUpperCase(Locale.ROOT));
    }

    private record DriverManagerDataSource(String url, String user, String password) implements DataSource {
        private DriverManagerDataSource(String url, String user, String password) {
            this.url = Objects.requireNonNull(url, "url must not be null");
            this.user = user;
            this.password = password;
        }

        @Override
        public Connection getConnection() throws SQLException {
            return DriverManager.getConnection(url, user, password);
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return DriverManager.getConnection(url, username, password);
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            throw new SQLFeatureNotSupportedException("unwrap is not supported");
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return false;
        }

        @Override
        public PrintWriter getLogWriter() {
            return DriverManager.getLogWriter();
        }

        @Override
        public void setLogWriter(PrintWriter out) {
            DriverManager.setLogWriter(out);
        }

        @Override
        public int getLoginTimeout() {
            return DriverManager.getLoginTimeout();
        }

        @Override
        public void setLoginTimeout(int seconds) {
            DriverManager.setLoginTimeout(seconds);
        }

        @Override
        public Logger getParentLogger() {
            return Logger.getLogger("global");
        }
    }
}

