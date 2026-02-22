package io.sqm.control;

/**
 * Configuration for built-in middleware rewrite rules.
 *
 * @param defaultLimitInjectionValue default LIMIT value injected by {@link BuiltInRewriteRule#LIMIT_INJECTION}
 * @param maxAllowedLimit optional maximum allowed LIMIT value; {@code null} disables max-limit policy
 * @param limitExcessMode behavior when explicit LIMIT exceeds configured max
 */
public record BuiltInRewriteSettings(
    long defaultLimitInjectionValue,
    Integer maxAllowedLimit,
    LimitExcessMode limitExcessMode
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
     * Returns default built-in rewrite settings.
     *
     * @return default settings
     */
    public static BuiltInRewriteSettings defaults() {
        return new BuiltInRewriteSettings(DEFAULT_LIMIT_INJECTION_VALUE, null, LimitExcessMode.DENY);
    }

    /**
     * Creates settings with a custom default injected LIMIT and no max-limit policy.
     *
     * @param defaultLimitInjectionValue default LIMIT value used by limit injection rewrite
     */
    public BuiltInRewriteSettings(long defaultLimitInjectionValue) {
        this(defaultLimitInjectionValue, null, LimitExcessMode.DENY);
    }

    /**
     * Validates settings invariants.
     *
     * @param defaultLimitInjectionValue default LIMIT value used by limit injection rewrite
     * @param maxAllowedLimit optional maximum allowed LIMIT value
     * @param limitExcessMode behavior when explicit LIMIT exceeds configured max
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
        if (maxAllowedLimit != null
            && limitExcessMode == LimitExcessMode.DENY
            && defaultLimitInjectionValue > maxAllowedLimit) {
            throw new IllegalArgumentException(
                "defaultLimitInjectionValue must be <= maxAllowedLimit when limitExcessMode is DENY"
            );
        }
    }
}
