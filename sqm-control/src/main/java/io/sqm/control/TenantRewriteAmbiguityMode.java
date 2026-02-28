package io.sqm.control;

/**
 * Behavior when tenant rewrite detects an ambiguous target resolution.
 */
public enum TenantRewriteAmbiguityMode {
    /**
     * Deny decision evaluation.
     */
    DENY,
    /**
     * Skip tenant rewrite for ambiguous targets.
     */
    SKIP
}
