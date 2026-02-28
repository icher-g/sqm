package io.sqm.control;

import java.util.Objects;

/**
 * Centralized external configuration keys used by SQL middleware runtime and control modules.
 *
 * <p>Every externally visible key should be defined here once, then referenced from factory/builders
 * instead of duplicating string literals.</p>
 */
public final class ConfigKeys {

    /**
     * Validation settings JSON text (property/env pair).
     */
    public static final Key VALIDATION_SETTINGS_JSON = Key.of("sqm.validation.settings.json", "SQM_VALIDATION_SETTINGS_JSON");

    /**
     * Validation settings YAML text (property/env pair).
     */
    public static final Key VALIDATION_SETTINGS_YAML = Key.of("sqm.validation.settings.yaml", "SQM_VALIDATION_SETTINGS_YAML");

    /**
     * Runtime schema source selector.
     */
    public static final Key SCHEMA_SOURCE = Key.of("sqm.middleware.schema.source", "SQM_MIDDLEWARE_SCHEMA_SOURCE");

    /**
     * Runtime path to default manual JSON schema.
     */
    public static final Key SCHEMA_DEFAULT_JSON_PATH = Key.of(
        "sqm.middleware.schema.defaultJson.path",
        "SQM_MIDDLEWARE_SCHEMA_DEFAULT_JSON_PATH"
    );

    /**
     * Runtime path to JSON schema source.
     */
    public static final Key SCHEMA_JSON_PATH = Key.of("sqm.middleware.schema.json.path", "SQM_MIDDLEWARE_SCHEMA_JSON_PATH");

    /**
     * Runtime JDBC URL for schema introspection.
     */
    public static final Key JDBC_URL = Key.of("sqm.middleware.jdbc.url", "SQM_MIDDLEWARE_JDBC_URL");

    /**
     * Runtime JDBC username for schema introspection.
     */
    public static final Key JDBC_USER = Key.of("sqm.middleware.jdbc.user", "SQM_MIDDLEWARE_JDBC_USER");

    /**
     * Runtime JDBC password for schema introspection.
     */
    public static final Key JDBC_PASSWORD = Key.of("sqm.middleware.jdbc.password", "SQM_MIDDLEWARE_JDBC_PASSWORD");

    /**
     * Runtime JDBC schema pattern.
     */
    public static final Key JDBC_SCHEMA_PATTERN = Key.of(
        "sqm.middleware.jdbc.schemaPattern",
        "SQM_MIDDLEWARE_JDBC_SCHEMA_PATTERN"
    );

    /**
     * Runtime JDBC driver class.
     */
    public static final Key JDBC_DRIVER = Key.of("sqm.middleware.jdbc.driver", "SQM_MIDDLEWARE_JDBC_DRIVER");

    /**
     * Enables/disables rewrite pipeline at runtime.
     */
    public static final Key REWRITE_ENABLED = Key.of("sqm.middleware.rewrite.enabled", "SQM_MIDDLEWARE_REWRITE_ENABLED");

    /**
     * Runtime max join count validation limit.
     */
    public static final Key VALIDATION_MAX_JOIN_COUNT = Key.of(
        "sqm.middleware.validation.maxJoinCount",
        "SQM_MIDDLEWARE_VALIDATION_MAX_JOIN_COUNT"
    );

    /**
     * Runtime max select columns validation limit.
     */
    public static final Key VALIDATION_MAX_SELECT_COLUMNS = Key.of(
        "sqm.middleware.validation.maxSelectColumns",
        "SQM_MIDDLEWARE_VALIDATION_MAX_SELECT_COLUMNS"
    );

    /**
     * Runtime tenant requirement mode for validation settings.
     */
    public static final Key VALIDATION_TENANT_REQUIREMENT_MODE = Key.of(
        "sqm.middleware.validation.tenantRequirementMode",
        "SQM_MIDDLEWARE_VALIDATION_TENANT_REQUIREMENT_MODE"
    );

    /**
     * Runtime max SQL length guardrail.
     */
    public static final Key GUARDRAILS_MAX_SQL_LENGTH = Key.of(
        "sqm.middleware.guardrails.maxSqlLength",
        "SQM_MIDDLEWARE_GUARDRAILS_MAX_SQL_LENGTH"
    );

    /**
     * Runtime evaluation timeout guardrail.
     */
    public static final Key GUARDRAILS_TIMEOUT_MILLIS = Key.of(
        "sqm.middleware.guardrails.timeoutMillis",
        "SQM_MIDDLEWARE_GUARDRAILS_TIMEOUT_MILLIS"
    );

    /**
     * Runtime max rows guardrail.
     */
    public static final Key GUARDRAILS_MAX_ROWS = Key.of(
        "sqm.middleware.guardrails.maxRows",
        "SQM_MIDDLEWARE_GUARDRAILS_MAX_ROWS"
    );

    /**
     * Runtime explain dry-run guardrail.
     */
    public static final Key GUARDRAILS_EXPLAIN_DRY_RUN = Key.of(
        "sqm.middleware.guardrails.explainDryRun",
        "SQM_MIDDLEWARE_GUARDRAILS_EXPLAIN_DRY_RUN"
    );

    /**
     * Runtime rewrite rules list.
     */
    public static final Key REWRITE_RULES = Key.of("sqm.middleware.rewrite.rules", "SQM_MIDDLEWARE_REWRITE_RULES");

    /**
     * Runtime default limit injection value for rewrite.
     */
    public static final Key REWRITE_DEFAULT_LIMIT_INJECTION_VALUE = Key.of(
        "sqm.middleware.rewrite.defaultLimitInjectionValue",
        "SQM_MIDDLEWARE_REWRITE_DEFAULT_LIMIT_INJECTION_VALUE"
    );

    /**
     * Runtime max allowed LIMIT setting for rewrite.
     */
    public static final Key REWRITE_MAX_ALLOWED_LIMIT = Key.of(
        "sqm.middleware.rewrite.maxAllowedLimit",
        "SQM_MIDDLEWARE_REWRITE_MAX_ALLOWED_LIMIT"
    );

    /**
     * Runtime limit excess mode for rewrite.
     */
    public static final Key REWRITE_LIMIT_EXCESS_MODE = Key.of(
        "sqm.middleware.rewrite.limitExcessMode",
        "SQM_MIDDLEWARE_REWRITE_LIMIT_EXCESS_MODE"
    );

    /**
     * Runtime default schema for qualification rewrite.
     */
    public static final Key REWRITE_QUALIFICATION_DEFAULT_SCHEMA = Key.of(
        "sqm.middleware.rewrite.qualificationDefaultSchema",
        "SQM_MIDDLEWARE_REWRITE_QUALIFICATION_DEFAULT_SCHEMA"
    );

    /**
     * Runtime qualification failure mode for rewrite.
     */
    public static final Key REWRITE_QUALIFICATION_FAILURE_MODE = Key.of(
        "sqm.middleware.rewrite.qualificationFailureMode",
        "SQM_MIDDLEWARE_REWRITE_QUALIFICATION_FAILURE_MODE"
    );

    /**
     * Runtime identifier normalization case mode for rewrite.
     */
    public static final Key REWRITE_IDENTIFIER_NORMALIZATION_CASE_MODE = Key.of(
        "sqm.middleware.rewrite.identifierNormalizationCaseMode",
        "SQM_MIDDLEWARE_REWRITE_IDENTIFIER_NORMALIZATION_CASE_MODE"
    );

    /**
     * Runtime tenant rewrite table policies list.
     *
     * <p>Format: {@code schema.table:tenant_column[:REQUIRED|OPTIONAL|SKIP],...}</p>
     */
    public static final Key REWRITE_TENANT_TABLE_POLICIES = Key.of(
        "sqm.middleware.rewrite.tenant.tablePolicies",
        "SQM_MIDDLEWARE_REWRITE_TENANT_TABLE_POLICIES"
    );

    /**
     * Runtime tenant rewrite fallback mode for missing table mappings.
     */
    public static final Key REWRITE_TENANT_FALLBACK_MODE = Key.of(
        "sqm.middleware.rewrite.tenant.fallbackMode",
        "SQM_MIDDLEWARE_REWRITE_TENANT_FALLBACK_MODE"
    );

    /**
     * Runtime tenant rewrite ambiguity mode for unresolved/ambiguous targets.
     */
    public static final Key REWRITE_TENANT_AMBIGUITY_MODE = Key.of(
        "sqm.middleware.rewrite.tenant.ambiguityMode",
        "SQM_MIDDLEWARE_REWRITE_TENANT_AMBIGUITY_MODE"
    );

    private ConfigKeys() {
    }

    /**
     * One external configuration entry, represented as JVM property + environment variable pair.
     *
     * @param property JVM system property name.
     * @param env      environment variable name.
     */
    public record Key(String property, String env) {
        /**
         * Creates a validated key pair.
         *
         * @param property JVM system property name.
         * @param env      environment variable name.
         * @return validated key pair.
         */
        public static Key of(String property, String env) {
            return new Key(
                Objects.requireNonNull(property, "property must not be null"),
                Objects.requireNonNull(env, "env must not be null")
            );
        }
    }
}

