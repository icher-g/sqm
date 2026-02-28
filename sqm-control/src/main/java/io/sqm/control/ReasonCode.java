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
     * Query denied due to semantic validation failures.
     */
    DENY_VALIDATION,

    /**
     * Query denied because tenant context is required but missing.
     */
    DENY_TENANT_REQUIRED,

    /**
     * Query denied because tenant table mapping is missing and fallback mode is DENY.
     */
    DENY_TENANT_MAPPING_MISSING,

    /**
     * Query denied because tenant table mapping is ambiguous and ambiguity mode is DENY.
     */
    DENY_TENANT_MAPPING_AMBIGUOUS,

    /**
     * Query denied because SQL text exceeds configured maximum length.
     */
    DENY_MAX_SQL_LENGTH,

    /**
     * Query denied because evaluation exceeded configured timeout.
     */
    DENY_TIMEOUT,

    /**
     * Query denied because LIMIT guardrail was violated.
     */
    DENY_MAX_ROWS,

    /**
     * Query was rewritten to enforce a limit.
     */
    REWRITE_LIMIT,

    /**
     * Query was rewritten to apply identifier qualification.
     */
    REWRITE_QUALIFICATION,

    /**
     * Query was rewritten to inject tenant predicate constraints.
     */
    REWRITE_TENANT_PREDICATE,

    /**
     * Query was rewritten to normalize identifier lexical form.
     */
    REWRITE_IDENTIFIER_NORMALIZATION,

    /**
     * Query was rewritten to a canonicalized form.
     */
    REWRITE_CANONICALIZATION,

    /**
     * Query was rewritten to EXPLAIN dry-run mode.
     */
    REWRITE_EXPLAIN_DRY_RUN
}
