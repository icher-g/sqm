package io.sqm.control;

/**
 * Global behavior when tenant rewrite cannot find a table mapping.
 */
public enum TenantRewriteFallbackMode {
    /**
     * Deny decision evaluation.
     */
    DENY,
    /**
     * Skip tenant rewrite for the unresolved table.
     */
    SKIP
}
