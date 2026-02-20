package io.sqm.core.control;

/**
 * Represents how SQL processing should be handled by middleware.
 */
public enum ExecutionMode {
    /**
     * Analyze SQL and return a decision without executing it.
     */
    ANALYZE,

    /**
     * Analyze SQL and allow downstream execution if policy checks pass.
     */
    EXECUTE
}
