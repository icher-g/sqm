package io.sqm.control;

import io.sqm.core.transform.IdentifierNormalizationCaseMode;

/**
 * Configuration for built-in middleware rewrite rules.
 *
 * @param defaultLimitInjectionValue      default LIMIT value injected by {@link BuiltInRewriteRule#LIMIT_INJECTION}
 * @param maxAllowedLimit                 optional maximum allowed LIMIT value; {@code null} disables max-limit policy
 * @param limitExcessMode                 behavior when explicit LIMIT exceeds configured max
 * @param qualificationDefaultSchema      optional preferred schema used to resolve ambiguous unqualified tables
 * @param qualificationFailureMode        behavior when schema qualification cannot be resolved deterministically
 * @param identifierNormalizationCaseMode case mode for unquoted identifier normalization
 */
public record BuiltInRewriteSettings(
    long defaultLimitInjectionValue,
    Integer maxAllowedLimit,
    LimitExcessMode limitExcessMode,
    String qualificationDefaultSchema,
    QualificationFailureMode qualificationFailureMode,
    IdentifierNormalizationCaseMode identifierNormalizationCaseMode
) {
    private static final long DEFAULT_LIMIT_INJECTION_VALUE = 1000L;
    private static final LimitExcessMode DEFAULT_LIMIT_EXCESS_MODE = LimitExcessMode.DENY;
    private static final QualificationFailureMode DEFAULT_QUALIFICATION_FAILURE_MODE = QualificationFailureMode.DENY;
    private static final IdentifierNormalizationCaseMode DEFAULT_IDENTIFIER_NORMALIZATION_CASE_MODE = IdentifierNormalizationCaseMode.LOWER;

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
        if (qualificationDefaultSchema != null && qualificationDefaultSchema.isBlank()) {
            qualificationDefaultSchema = null;
        }
        if (maxAllowedLimit != null
            && limitExcessMode == LimitExcessMode.DENY
            && defaultLimitInjectionValue > maxAllowedLimit) {
            throw new IllegalArgumentException(
                "defaultLimitInjectionValue must be <= maxAllowedLimit when limitExcessMode is DENY"
            );
        }
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

        private Builder() {
        }

        private Builder(BuiltInRewriteSettings source) {
            this.defaultLimitInjectionValue = source.defaultLimitInjectionValue();
            this.maxAllowedLimit = source.maxAllowedLimit();
            this.limitExcessMode = source.limitExcessMode();
            this.qualificationDefaultSchema = source.qualificationDefaultSchema();
            this.qualificationFailureMode = source.qualificationFailureMode();
            this.identifierNormalizationCaseMode = source.identifierNormalizationCaseMode();
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
                identifierNormalizationCaseMode
            );
        }
    }
}
