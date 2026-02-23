package io.sqm.control;

/**
 * Configuration for built-in middleware rewrite rules.
 *
 * @param defaultLimitInjectionValue default LIMIT value injected by {@link BuiltInRewriteRule#LIMIT_INJECTION}
 * @param maxAllowedLimit optional maximum allowed LIMIT value; {@code null} disables max-limit policy
 * @param limitExcessMode behavior when explicit LIMIT exceeds configured max
 * @param qualificationDefaultSchema optional preferred schema used to resolve ambiguous unqualified tables
 * @param qualificationFailureMode behavior when schema qualification cannot be resolved deterministically
 */
public record BuiltInRewriteSettings(
    long defaultLimitInjectionValue,
    Integer maxAllowedLimit,
    LimitExcessMode limitExcessMode,
    String qualificationDefaultSchema,
    QualificationFailureMode qualificationFailureMode
) {
    private static final long DEFAULT_LIMIT_INJECTION_VALUE = 1000L;

    /**
     * Behavior mode for explicit LIMIT values that exceed configured max.
     */
    public enum LimitExcessMode {
        /**
         * Deny the query when explicit LIMIT exceeds configured max.
         */
        DENY,
        /**
         * Clamp the explicit LIMIT down to the configured max.
         */
        CLAMP
    }

    /**
     * Behavior mode for qualification failures (missing or ambiguous table resolution).
     */
    public enum QualificationFailureMode {
        /**
         * Leave the table reference unchanged when qualification cannot be resolved deterministically.
         */
        SKIP,
        /**
         * Deny the query when qualification cannot be resolved deterministically.
         */
        DENY
    }

    /**
     * Returns default built-in rewrite settings.
     *
     * @return default settings
     */
    public static BuiltInRewriteSettings defaults() {
        return new BuiltInRewriteSettings(
            DEFAULT_LIMIT_INJECTION_VALUE,
            null,
            LimitExcessMode.DENY,
            null,
            QualificationFailureMode.DENY
        );
    }

    /**
     * Creates settings with a custom default injected LIMIT and no max-limit policy.
     *
     * @param defaultLimitInjectionValue default LIMIT value used by limit injection rewrite
     */
    public BuiltInRewriteSettings(long defaultLimitInjectionValue) {
        this(defaultLimitInjectionValue, null, LimitExcessMode.DENY, null, QualificationFailureMode.DENY);
    }

    /**
     * Creates settings with explicit LIMIT policy and default qualification policy.
     *
     * @param defaultLimitInjectionValue default LIMIT value used by limit injection rewrite
     * @param maxAllowedLimit optional maximum allowed LIMIT value
     * @param limitExcessMode behavior when explicit LIMIT exceeds configured max
     */
    public BuiltInRewriteSettings(
        long defaultLimitInjectionValue,
        Integer maxAllowedLimit,
        LimitExcessMode limitExcessMode
    ) {
        this(defaultLimitInjectionValue, maxAllowedLimit, limitExcessMode, null, QualificationFailureMode.DENY);
    }

    /**
     * Validates settings invariants.
     *
     * @param defaultLimitInjectionValue default LIMIT value used by limit injection rewrite
     * @param maxAllowedLimit optional maximum allowed LIMIT value
     * @param limitExcessMode behavior when explicit LIMIT exceeds configured max
     * @param qualificationDefaultSchema optional preferred schema used to resolve ambiguous unqualified tables
     * @param qualificationFailureMode behavior when qualification fails (missing/ambiguous)
     */
    public BuiltInRewriteSettings {
        if (defaultLimitInjectionValue <= 0) {
            throw new IllegalArgumentException("defaultLimitInjectionValue must be > 0");
        }
        if (maxAllowedLimit != null && maxAllowedLimit <= 0) {
            throw new IllegalArgumentException("maxAllowedLimit must be > 0");
        }
        if (limitExcessMode == null) {
            limitExcessMode = LimitExcessMode.DENY;
        }
        if (qualificationFailureMode == null) {
            qualificationFailureMode = QualificationFailureMode.DENY;
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
}
