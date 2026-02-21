package io.sqm.control;

/**
 * Stable, machine-readable reason codes for middleware decisions.
 */
public enum ReasonCode {
    /**
     * No specific reason code.
     */
    NONE,

    /**
     * DDL statement denied by policy.
     */
    DENY_DDL,

    /**
     * DML statement denied by policy.
     */
    DENY_DML,

    /**
     * Table usage denied by policy.
     */
    DENY_TABLE,

    /**
     * Column usage denied by policy.
     */
    DENY_COLUMN,

    /**
     * Function usage denied by policy.
     */
    DENY_FUNCTION,

    /**
     * Query denied due to join-count limit.
     */
    DENY_MAX_JOINS,

    /**
     * Query denied due to projected-column limit.
     */
    DENY_MAX_SELECT_COLUMNS,

    /**
     * Query contains a feature unsupported by selected dialect.
     */
    DENY_UNSUPPORTED_DIALECT_FEATURE,

    /**
     * Query processing failed due to an internal pipeline error.
     */
    DENY_PIPELINE_ERROR,

    /**
     * Query was rewritten to enforce a limit.
     */
    REWRITE_LIMIT,

    /**
     * Query was rewritten to apply identifier qualification.
     */
    REWRITE_QUALIFICATION
}
