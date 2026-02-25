package io.sqm.control;

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
