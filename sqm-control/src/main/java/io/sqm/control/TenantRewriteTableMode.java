package io.sqm.control;

/**
 * Per-table tenant rewrite behavior.
 */
public enum TenantRewriteTableMode {
    /**
     * Tenant predicate is mandatory for this table.
     */
    REQUIRED,
    /**
     * Tenant predicate is best-effort for this table.
     */
    OPTIONAL,
    /**
     * Tenant predicate should not be injected for this table.
     */
    SKIP
}
