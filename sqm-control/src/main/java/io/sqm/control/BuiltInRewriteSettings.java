package io.sqm.control;

import io.sqm.core.transform.IdentifierNormalizationCaseMode;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration for built-in middleware rewrite rules.
 *
 * @param defaultLimitInjectionValue      default LIMIT value injected by {@link BuiltInRewriteRule#LIMIT_INJECTION}
 * @param maxAllowedLimit                 optional maximum allowed LIMIT value; {@code null} disables max-limit policy
 * @param limitExcessMode                 behavior when explicit LIMIT exceeds configured max
 * @param qualificationDefaultSchema      optional preferred schema used to resolve ambiguous unqualified tables
 * @param qualificationFailureMode        behavior when schema qualification cannot be resolved deterministically
 * @param identifierNormalizationCaseMode case mode for unquoted identifier normalization
 * @param tenantTablePolicies             tenant rewrite policies keyed by fully-qualified table name ({@code schema.table})
 * @param tenantFallbackMode              fallback behavior when tenant mapping is missing
 * @param tenantAmbiguityMode             behavior when tenant target resolution is ambiguous
 */
public record BuiltInRewriteSettings(
    long defaultLimitInjectionValue,
    Integer maxAllowedLimit,
    LimitExcessMode limitExcessMode,
    String qualificationDefaultSchema,
    QualificationFailureMode qualificationFailureMode,
    IdentifierNormalizationCaseMode identifierNormalizationCaseMode,
    Map<String, TenantRewriteTablePolicy> tenantTablePolicies,
    TenantRewriteFallbackMode tenantFallbackMode,
    TenantRewriteAmbiguityMode tenantAmbiguityMode
) {
    private static final long DEFAULT_LIMIT_INJECTION_VALUE = 1000L;
    private static final LimitExcessMode DEFAULT_LIMIT_EXCESS_MODE = LimitExcessMode.DENY;
    private static final QualificationFailureMode DEFAULT_QUALIFICATION_FAILURE_MODE = QualificationFailureMode.DENY;
    private static final IdentifierNormalizationCaseMode DEFAULT_IDENTIFIER_NORMALIZATION_CASE_MODE = IdentifierNormalizationCaseMode.LOWER;
    private static final TenantRewriteFallbackMode DEFAULT_TENANT_FALLBACK_MODE = TenantRewriteFallbackMode.DENY;
    private static final TenantRewriteAmbiguityMode DEFAULT_TENANT_AMBIGUITY_MODE = TenantRewriteAmbiguityMode.DENY;

    /**
     * Creates a builder initialized with default settings values.
     *
     * @return settings builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a builder initialized from existing settings.
     *
     * @param source source settings
     * @return settings builder
     */
    public static Builder builder(BuiltInRewriteSettings source) {
        return new Builder(source);
    }

    /**
     * Validates settings invariants.
     *
     * @param defaultLimitInjectionValue      default LIMIT value used by limit injection rewrite
     * @param maxAllowedLimit                 optional maximum allowed LIMIT value
     * @param limitExcessMode                 behavior when explicit LIMIT exceeds configured max
     * @param qualificationDefaultSchema      optional preferred schema used to resolve ambiguous unqualified tables
     * @param qualificationFailureMode        behavior when qualification fails (missing/ambiguous)
     * @param identifierNormalizationCaseMode case mode for unquoted identifier normalization
     * @param tenantTablePolicies             tenant rewrite policies keyed by fully-qualified table name ({@code schema.table})
     * @param tenantFallbackMode              fallback behavior when tenant mapping is missing
     * @param tenantAmbiguityMode             behavior when tenant target resolution is ambiguous
     */
    public BuiltInRewriteSettings {
        if (defaultLimitInjectionValue <= 0) {
            throw new IllegalArgumentException("defaultLimitInjectionValue must be > 0");
        }
        if (maxAllowedLimit != null && maxAllowedLimit <= 0) {
            throw new IllegalArgumentException("maxAllowedLimit must be > 0");
        }
        if (limitExcessMode == null) {
            limitExcessMode = DEFAULT_LIMIT_EXCESS_MODE;
        }
        if (qualificationFailureMode == null) {
            qualificationFailureMode = DEFAULT_QUALIFICATION_FAILURE_MODE;
        }
        if (identifierNormalizationCaseMode == null) {
            identifierNormalizationCaseMode = DEFAULT_IDENTIFIER_NORMALIZATION_CASE_MODE;
        }
        if (tenantFallbackMode == null) {
            tenantFallbackMode = DEFAULT_TENANT_FALLBACK_MODE;
        }
        if (tenantAmbiguityMode == null) {
            tenantAmbiguityMode = DEFAULT_TENANT_AMBIGUITY_MODE;
        }
        if (qualificationDefaultSchema != null && qualificationDefaultSchema.isBlank()) {
            qualificationDefaultSchema = null;
        }
        tenantTablePolicies = normalizeTenantTablePolicies(tenantTablePolicies);
        if (maxAllowedLimit != null
            && limitExcessMode == LimitExcessMode.DENY
            && defaultLimitInjectionValue > maxAllowedLimit) {
            throw new IllegalArgumentException(
                "defaultLimitInjectionValue must be <= maxAllowedLimit when limitExcessMode is DENY"
            );
        }
    }

    private static Map<String, TenantRewriteTablePolicy> normalizeTenantTablePolicies(Map<String, TenantRewriteTablePolicy> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }

        var normalized = new LinkedHashMap<String, TenantRewriteTablePolicy>(source.size());
        source.forEach((table, policy) -> {
            if (table == null || table.isBlank()) {
                throw new IllegalArgumentException("tenantTablePolicies table key must not be blank");
            }
            var normalizedTable = table.trim().toLowerCase(java.util.Locale.ROOT);
            if (normalizedTable.indexOf('.') < 1 || normalizedTable.endsWith(".")) {
                throw new IllegalArgumentException(
                    "tenantTablePolicies key must be fully-qualified schema.table: " + table
                );
            }
            normalized.put(normalizedTable, Objects.requireNonNull(policy, "tenantTablePolicies policy must not be null"));
        });
        return Map.copyOf(normalized);
    }

    /**
     * Returns default built-in rewrite settings.
     *
     * @return default settings
     */
    public static BuiltInRewriteSettings defaults() {
        return builder().build();
    }

    /**
     * Builder for {@link BuiltInRewriteSettings}.
     */
    public static final class Builder {
        private long defaultLimitInjectionValue = DEFAULT_LIMIT_INJECTION_VALUE;
        private Integer maxAllowedLimit;
        private LimitExcessMode limitExcessMode = DEFAULT_LIMIT_EXCESS_MODE;
        private String qualificationDefaultSchema;
        private QualificationFailureMode qualificationFailureMode = DEFAULT_QUALIFICATION_FAILURE_MODE;
        private IdentifierNormalizationCaseMode identifierNormalizationCaseMode = DEFAULT_IDENTIFIER_NORMALIZATION_CASE_MODE;
        private final Map<String, TenantRewriteTablePolicy> tenantTablePolicies = new LinkedHashMap<>();
        private TenantRewriteFallbackMode tenantFallbackMode = DEFAULT_TENANT_FALLBACK_MODE;
        private TenantRewriteAmbiguityMode tenantAmbiguityMode = DEFAULT_TENANT_AMBIGUITY_MODE;

        private Builder() {
        }

        private Builder(BuiltInRewriteSettings source) {
            this.defaultLimitInjectionValue = source.defaultLimitInjectionValue();
            this.maxAllowedLimit = source.maxAllowedLimit();
            this.limitExcessMode = source.limitExcessMode();
            this.qualificationDefaultSchema = source.qualificationDefaultSchema();
            this.qualificationFailureMode = source.qualificationFailureMode();
            this.identifierNormalizationCaseMode = source.identifierNormalizationCaseMode();
            this.tenantTablePolicies.putAll(source.tenantTablePolicies());
            this.tenantFallbackMode = source.tenantFallbackMode();
            this.tenantAmbiguityMode = source.tenantAmbiguityMode();
        }

        /**
         * Sets default LIMIT value injected by {@link BuiltInRewriteRule#LIMIT_INJECTION}.
         *
         * @param value injected LIMIT value
         * @return builder instance
         */
        public Builder defaultLimitInjectionValue(long value) {
            this.defaultLimitInjectionValue = value;
            return this;
        }

        /**
         * Sets optional maximum allowed LIMIT value.
         *
         * @param value maximum allowed LIMIT, or {@code null} to disable max-limit policy
         * @return builder instance
         */
        public Builder maxAllowedLimit(Integer value) {
            this.maxAllowedLimit = value;
            return this;
        }

        /**
         * Sets behavior for explicit LIMIT values that exceed configured max limit.
         *
         * @param mode exceed-limit behavior
         * @return builder instance
         */
        public Builder limitExcessMode(LimitExcessMode mode) {
            this.limitExcessMode = mode;
            return this;
        }

        /**
         * Sets preferred schema used to resolve ambiguous unqualified tables.
         *
         * @param schema schema name
         * @return builder instance
         */
        public Builder qualificationDefaultSchema(String schema) {
            this.qualificationDefaultSchema = schema;
            return this;
        }

        /**
         * Sets behavior when schema qualification cannot be resolved deterministically.
         *
         * @param mode qualification failure behavior
         * @return builder instance
         */
        public Builder qualificationFailureMode(QualificationFailureMode mode) {
            this.qualificationFailureMode = mode;
            return this;
        }

        /**
         * Sets case mode for unquoted identifier normalization.
         *
         * @param mode identifier normalization case mode
         * @return builder instance
         */
        public Builder identifierNormalizationCaseMode(IdentifierNormalizationCaseMode mode) {
            this.identifierNormalizationCaseMode = mode;
            return this;
        }

        /**
         * Replaces tenant rewrite table policies map.
         *
         * @param policies table policy map keyed by {@code schema.table}
         * @return builder instance
         */
        public Builder tenantTablePolicies(Map<String, TenantRewriteTablePolicy> policies) {
            this.tenantTablePolicies.clear();
            if (policies != null) {
                this.tenantTablePolicies.putAll(policies);
            }
            return this;
        }

        /**
         * Adds one tenant rewrite table policy.
         *
         * @param qualifiedTable fully-qualified table name ({@code schema.table})
         * @param policy tenant table policy
         * @return builder instance
         */
        public Builder tenantTablePolicy(String qualifiedTable, TenantRewriteTablePolicy policy) {
            this.tenantTablePolicies.put(qualifiedTable, policy);
            return this;
        }

        /**
         * Sets fallback behavior when tenant mapping is missing.
         *
         * @param mode tenant fallback mode
         * @return builder instance
         */
        public Builder tenantFallbackMode(TenantRewriteFallbackMode mode) {
            this.tenantFallbackMode = mode;
            return this;
        }

        /**
         * Sets behavior when tenant target resolution is ambiguous.
         *
         * @param mode tenant ambiguity mode
         * @return builder instance
         */
        public Builder tenantAmbiguityMode(TenantRewriteAmbiguityMode mode) {
            this.tenantAmbiguityMode = mode;
            return this;
        }

        /**
         * Builds immutable rewrite settings.
         *
         * @return immutable settings
         */
        public BuiltInRewriteSettings build() {
            return new BuiltInRewriteSettings(
                defaultLimitInjectionValue,
                maxAllowedLimit,
                limitExcessMode,
                qualificationDefaultSchema,
                qualificationFailureMode,
                identifierNormalizationCaseMode,
                tenantTablePolicies,
                tenantFallbackMode,
                tenantAmbiguityMode
            );
        }
    }
}
