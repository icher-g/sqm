package io.sqm.middleware.api;

/**
 * Transport-neutral SQL execution mode.
 */
public enum ExecutionModeDto {
    /**
     * Analyze SQL and return a decision without executing it.
     */
    ANALYZE,

    /**
     * Analyze SQL and allow downstream execution if policy checks pass.
     */
    EXECUTE
}
