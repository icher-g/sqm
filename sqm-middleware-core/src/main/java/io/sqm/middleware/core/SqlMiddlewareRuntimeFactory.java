package io.sqm.middleware.core;

import io.sqm.catalog.jdbc.JdbcSchemaProvider;
import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.snapshot.JsonSchemaProvider;
import io.sqm.control.*;
import io.sqm.middleware.api.SqlMiddlewareService;
import io.sqm.validate.schema.SchemaValidationLimits;
import io.sqm.validate.schema.SchemaValidationSettings;

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

        var builder = SqlMiddlewareConfig.builder(schema);

        applyValidationSettings(builder);
        applyGuardrails(builder);

        if (readBoolean("sqm.middleware.rewrite.enabled", "SQM_MIDDLEWARE_REWRITE_ENABLED", true)) {
            applyRewriteCustomizations(builder);
            return SqlMiddlewareServices.create(builder.buildValidationAndRewriteConfig());
        }

        return SqlMiddlewareServices.create(builder.buildValidationConfig());
    }

    private static CatalogSchema loadSchema() {
        var source = readString("sqm.middleware.schema.source", "SQM_MIDDLEWARE_SCHEMA_SOURCE", "manual")
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
            "sqm.middleware.schema.defaultJson.path",
            "SQM_MIDDLEWARE_SCHEMA_DEFAULT_JSON_PATH",
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
        var path = readString("sqm.middleware.schema.json.path", "SQM_MIDDLEWARE_SCHEMA_JSON_PATH", null);
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("JSON schema source requires sqm.middleware.schema.json.path (or SQM_MIDDLEWARE_SCHEMA_JSON_PATH)");
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
        var url = required("sqm.middleware.jdbc.url", "SQM_MIDDLEWARE_JDBC_URL");
        var user = readString("sqm.middleware.jdbc.user", "SQM_MIDDLEWARE_JDBC_USER", "");
        var password = readString("sqm.middleware.jdbc.password", "SQM_MIDDLEWARE_JDBC_PASSWORD", "");
        var schemaPattern = readString("sqm.middleware.jdbc.schemaPattern", "SQM_MIDDLEWARE_JDBC_SCHEMA_PATTERN", null);
        var driverClass = readString("sqm.middleware.jdbc.driver", "SQM_MIDDLEWARE_JDBC_DRIVER", null);

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

    private static void applyValidationSettings(SqlMiddlewareConfig.Builder builder) {
        Integer maxJoinCount = readIntNullable("sqm.middleware.validation.maxJoinCount", "SQM_MIDDLEWARE_VALIDATION_MAX_JOIN_COUNT");
        Integer maxSelectColumns = readIntNullable("sqm.middleware.validation.maxSelectColumns", "SQM_MIDDLEWARE_VALIDATION_MAX_SELECT_COLUMNS");

        if (maxJoinCount == null && maxSelectColumns == null) {
            return;
        }

        var limitsBuilder = SchemaValidationLimits.builder();
        if (maxJoinCount != null) {
            limitsBuilder.maxJoinCount(maxJoinCount);
        }
        if (maxSelectColumns != null) {
            limitsBuilder.maxSelectColumns(maxSelectColumns);
        }

        builder.validationSettings(
            SchemaValidationSettings.builder()
                .limits(limitsBuilder.build())
                .build()
        );
    }

    private static void applyGuardrails(SqlMiddlewareConfig.Builder builder) {
        Integer maxSqlLength = readIntNullable("sqm.middleware.guardrails.maxSqlLength", "SQM_MIDDLEWARE_GUARDRAILS_MAX_SQL_LENGTH");
        Long timeoutMillis = readLongNullable("sqm.middleware.guardrails.timeoutMillis", "SQM_MIDDLEWARE_GUARDRAILS_TIMEOUT_MILLIS");
        Integer maxRows = readIntNullable("sqm.middleware.guardrails.maxRows", "SQM_MIDDLEWARE_GUARDRAILS_MAX_ROWS");
        boolean explainDryRun = readBoolean("sqm.middleware.guardrails.explainDryRun", "SQM_MIDDLEWARE_GUARDRAILS_EXPLAIN_DRY_RUN", false);

        if (maxSqlLength == null && timeoutMillis == null && maxRows == null && !explainDryRun) {
            return;
        }

        builder.guardrails(new RuntimeGuardrails(maxSqlLength, timeoutMillis, maxRows, explainDryRun));
    }

    private static void applyRewriteCustomizations(SqlMiddlewareConfig.Builder builder) {
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
        var raw = readString("sqm.middleware.rewrite.rules", "SQM_MIDDLEWARE_REWRITE_RULES", null);
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

        Long defaultLimit = readLongNullable("sqm.middleware.rewrite.defaultLimitInjectionValue", "SQM_MIDDLEWARE_REWRITE_DEFAULT_LIMIT_INJECTION_VALUE");
        if (defaultLimit != null) {
            settingsBuilder.defaultLimitInjectionValue(defaultLimit);
            configured = true;
        }

        Integer maxAllowedLimit = readIntNullable("sqm.middleware.rewrite.maxAllowedLimit", "SQM_MIDDLEWARE_REWRITE_MAX_ALLOWED_LIMIT");
        if (maxAllowedLimit != null) {
            settingsBuilder.maxAllowedLimit(maxAllowedLimit);
            configured = true;
        }

        var limitExcessMode = readEnumNullable("sqm.middleware.rewrite.limitExcessMode", "SQM_MIDDLEWARE_REWRITE_LIMIT_EXCESS_MODE", LimitExcessMode.class);
        if (limitExcessMode != null) {
            settingsBuilder.limitExcessMode(limitExcessMode);
            configured = true;
        }

        var qualificationDefaultSchema = readString("sqm.middleware.rewrite.qualificationDefaultSchema", "SQM_MIDDLEWARE_REWRITE_QUALIFICATION_DEFAULT_SCHEMA", null);
        if (qualificationDefaultSchema != null && !qualificationDefaultSchema.isBlank()) {
            settingsBuilder.qualificationDefaultSchema(qualificationDefaultSchema);
            configured = true;
        }

        var qualificationFailureMode = readEnumNullable("sqm.middleware.rewrite.qualificationFailureMode", "SQM_MIDDLEWARE_REWRITE_QUALIFICATION_FAILURE_MODE", QualificationFailureMode.class);
        if (qualificationFailureMode != null) {
            settingsBuilder.qualificationFailureMode(qualificationFailureMode);
            configured = true;
        }

        var identifierCase = readString("sqm.middleware.rewrite.identifierNormalizationCaseMode", "SQM_MIDDLEWARE_REWRITE_IDENTIFIER_NORMALIZATION_CASE_MODE", null);
        if (identifierCase != null && !identifierCase.isBlank()) {
            settingsBuilder.identifierNormalizationCaseMode(valueOf(identifierCase.toUpperCase(Locale.ROOT)));
            configured = true;
        }

        return configured ? settingsBuilder.build() : null;
    }

    private static String required(String propertyKey, String envKey) {
        var value = readString(propertyKey, envKey, null);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required configuration: " + propertyKey + " / " + envKey);
        }
        return value;
    }

    private static String readString(String propertyKey, String envKey, String defaultValue) {
        var value = System.getProperty(propertyKey);
        if (value == null || value.isBlank()) {
            value = System.getenv(envKey);
        }
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }

    private static boolean readBoolean(String propertyKey, String envKey, boolean defaultValue) {
        var raw = readString(propertyKey, envKey, null);
        return raw == null ? defaultValue : Boolean.parseBoolean(raw);
    }

    private static Integer readIntNullable(String propertyKey, String envKey) {
        var raw = readString(propertyKey, envKey, null);
        return raw == null ? null : Integer.valueOf(raw);
    }

    private static Long readLongNullable(String propertyKey, String envKey) {
        var raw = readString(propertyKey, envKey, null);
        return raw == null ? null : Long.valueOf(raw);
    }

    private static <E extends Enum<E>> E readEnumNullable(String propertyKey, String envKey, Class<E> enumType) {
        Objects.requireNonNull(enumType, "enumType must not be null");
        var raw = readString(propertyKey, envKey, null);
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
