package io.sqm.control;

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
