package io.sqm.middleware.core;

import io.sqm.control.*;
import io.sqm.control.audit.FileAuditEventPublisher;
import io.sqm.control.audit.LoggingAuditEventPublisher;
import io.sqm.middleware.api.*;
import io.sqm.validate.schema.SchemaValidationLimits;
import io.sqm.validate.schema.SchemaValidationSettings;
import io.sqm.validate.schema.SchemaValidationSettingsLoader;
import io.sqm.validate.schema.TenantRequirementMode;

import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.sqm.core.transform.IdentifierNormalizationCaseMode.valueOf;

/**
 * Shared runtime factory for building middleware service instances from external configuration.
 */
public final class SqlMiddlewareRuntimeFactory {

    private SqlMiddlewareRuntimeFactory() {
    }

    /**
     * Creates middleware service from system properties/environment.
     *
     * @return configured middleware service
     */
    public static SqlMiddlewareService createFromEnvironment() {
        return createRuntimeFromEnvironment().service();
    }

    /**
     * Creates runtime container from system properties/environment.
     *
     * @return runtime container with service and bootstrap diagnostics
     */
    public static SqlMiddlewareRuntime createRuntimeFromEnvironment() {
        validateProductionModeConfiguration();

        var schemaSource = readString(ConfigKeys.SCHEMA_SOURCE, "manual")
            .trim()
            .toLowerCase(Locale.ROOT);

        var failFast = readBoolean(ConfigKeys.SCHEMA_BOOTSTRAP_FAIL_FAST, true);

        var schemaLoader = new SchemaBootstrapLoader(SqlMiddlewareRuntimeFactory::readString);
        var bootstrap = schemaLoader.bootstrap(schemaSource, failFast);
        if (!bootstrap.ready()) {
            var message = bootstrap.degradedMessage();
            var degradedService = applyTelemetry(applyFlowControl(new SchemaUnavailableSqlMiddlewareService(message)));
            return new SqlMiddlewareRuntime(
                degradedService,
                SchemaBootstrapStatus.degraded(schemaSource, "schema source unavailable", message)
            );
        }

        var schemaLoad = bootstrap.schemaLoad();
        var builder = SqlDecisionServiceConfig.builder(schemaLoad.schema());

        applyValidationSettings(builder);
        applyGuardrails(builder);
        applyAuditPublisher(builder);

        var service = readBoolean(ConfigKeys.REWRITE_ENABLED, true)
            ? createRewriteEnabledServiceWithCustomizations(builder)
            : SqlMiddlewareServices.create(builder.buildValidationConfig());

        service = applyFlowControl(service);

        return new SqlMiddlewareRuntime(
            applyTelemetry(service),
            SchemaBootstrapStatus.ready(schemaSource, schemaLoad.description())
        );
    }

    private static SqlMiddlewareService createRewriteEnabledServiceWithCustomizations(SqlDecisionServiceConfig.Builder builder) {
        applyRewriteCustomizations(builder);
        return SqlMiddlewareServices.create(builder.buildValidationAndRewriteConfig());
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

    private static void validateProductionModeConfiguration() {
        if (!isProductionMode()) {
            return;
        }

        var configuredSchemaSource = readRaw(ConfigKeys.SCHEMA_SOURCE);
        if (configuredSchemaSource == null || configuredSchemaSource.isBlank()) {
            throw new IllegalStateException(
                "Production mode requires explicit schema source via %s or %s. " +
                    "Set one of: manual, json, jdbc.".formatted(
                        ConfigKeys.SCHEMA_SOURCE.property(),
                        ConfigKeys.SCHEMA_SOURCE.env()
                    )
            );
        }

        var source = configuredSchemaSource.trim().toLowerCase(Locale.ROOT);
        if ("manual".equals(source)) {
            var configuredDefaultPath = readRaw(ConfigKeys.SCHEMA_DEFAULT_JSON_PATH);
            if (configuredDefaultPath == null || configuredDefaultPath.isBlank()) {
                throw new IllegalStateException(
                    "Production mode disallows bundled fallback schema for manual source. " +
                        "Configure %s or %s to an explicit schema file path.".formatted(
                            ConfigKeys.SCHEMA_DEFAULT_JSON_PATH.property(),
                            ConfigKeys.SCHEMA_DEFAULT_JSON_PATH.env()
                        )
                );
            }
        }
    }

    private static void applyAuditPublisher(SqlDecisionServiceConfig.Builder builder) {
        var mode = readString(ConfigKeys.AUDIT_PUBLISHER_MODE, "noop")
            .trim()
            .toLowerCase(Locale.ROOT);

        switch (mode) {
            case "noop" -> builder.auditPublisher(AuditEventPublisher.noop());
            case "logging" -> {
                var loggerName = readString(ConfigKeys.AUDIT_LOGGER_NAME, "io.sqm.middleware.audit");
                var level = readLogLevelNullable(ConfigKeys.AUDIT_LOGGER_LEVEL);
                var logger = Logger.getLogger(loggerName);
                builder.auditPublisher(level == null
                    ? LoggingAuditEventPublisher.of(logger)
                    : LoggingAuditEventPublisher.of(logger, level));
            }
            case "file" -> {
                var path = required(ConfigKeys.AUDIT_FILE_PATH);
                builder.auditPublisher(FileAuditEventPublisher.of(Path.of(path)));
            }
            default -> throw new IllegalArgumentException(
                "Unsupported audit publisher mode: " + mode + ". Supported: noop,logging,file"
            );
        }
    }

    private static SqlMiddlewareService applyTelemetry(SqlMiddlewareService service) {
        if (!readBoolean(ConfigKeys.METRICS_ENABLED, false)) {
            return service;
        }
        var loggerName = readString(ConfigKeys.METRICS_LOGGER_NAME, "io.sqm.middleware.metrics");
        var level = readLogLevelNullable(ConfigKeys.METRICS_LOGGER_LEVEL);
        var logger = Logger.getLogger(loggerName);
        var telemetry = level == null
            ? LoggingMiddlewareTelemetry.of(logger)
            : LoggingMiddlewareTelemetry.of(logger, level);
        return new ObservedSqlMiddlewareService(service, telemetry);
    }

    private static SqlMiddlewareService applyFlowControl(SqlMiddlewareService service) {
        Integer maxInFlight = readIntNullable(ConfigKeys.HOST_MAX_IN_FLIGHT);
        Long acquireTimeoutMillis = readLongNullable(ConfigKeys.HOST_ACQUIRE_TIMEOUT_MILLIS);
        Long requestTimeoutMillis = readLongNullable(ConfigKeys.HOST_REQUEST_TIMEOUT_MILLIS);
        if (maxInFlight == null && acquireTimeoutMillis == null && requestTimeoutMillis == null) {
            return service;
        }
        return new FlowControlSqlMiddlewareService(
            service,
            maxInFlight,
            acquireTimeoutMillis,
            requestTimeoutMillis
        );
    }

    private static boolean isProductionMode() {
        if (readBoolean(ConfigKeys.PRODUCTION_MODE, false)) {
            return true;
        }

        var runtimeMode = readString(ConfigKeys.RUNTIME_MODE, "dev");
        if (runtimeMode != null && !runtimeMode.isBlank()) {
            var normalized = runtimeMode.trim().toLowerCase(Locale.ROOT);
            if ("production".equals(normalized) || "prod".equals(normalized)) {
                return true;
            }
        }

        var springProfiles = readString(
            ConfigKeys.Key.of("spring.profiles.active", "SPRING_PROFILES_ACTIVE"),
            null
        );
        if (springProfiles == null || springProfiles.isBlank()) {
            return false;
        }

        return Arrays.stream(springProfiles.split(","))
            .map(String::trim)
            .map(s -> s.toLowerCase(Locale.ROOT))
            .anyMatch(profile -> "production".equals(profile) || "prod".equals(profile));
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

    private static String readRaw(ConfigKeys.Key key) {
        var value = System.getProperty(key.property());
        if (value != null) {
            return value;
        }
        return System.getenv(key.env());
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

    private static Level readLogLevelNullable(ConfigKeys.Key key) {
        var raw = readString(key, null);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return Level.parse(raw.trim().toUpperCase(Locale.ROOT));
    }

    private record SchemaUnavailableSqlMiddlewareService(String errorMessage) implements SqlMiddlewareService {
        private SchemaUnavailableSqlMiddlewareService(String errorMessage) {
            this.errorMessage = Objects.requireNonNull(errorMessage, "errorMessage must not be null");
        }

        private static DecisionResultDto denied(String message) {
            return new DecisionResultDto(
                DecisionKindDto.DENY,
                ReasonCodeDto.DENY_PIPELINE_ERROR,
                message,
                null,
                List.of(),
                null,
                null
            );
        }

        @Override
        public DecisionResultDto analyze(AnalyzeRequest request) {
            return denied(errorMessage);
        }

        @Override
        public DecisionResultDto enforce(EnforceRequest request) {
            return denied(errorMessage);
        }

        @Override
        public DecisionExplanationDto explainDecision(ExplainRequest request) {
            var result = denied(errorMessage);
            return new DecisionExplanationDto(result, "Schema bootstrap failed; service is running in degraded not-ready mode.");
        }
    }
}
