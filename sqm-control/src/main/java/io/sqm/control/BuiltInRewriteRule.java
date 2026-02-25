package io.sqm.control;

import io.sqm.control.rewrite.BuiltInSqlRewriters;

/**
 * Built-in AST rewrite rules planned for SQL middleware policy enforcement.
 *
 * <p>Only rules that are implemented and wired in the current version are selectable
 * through {@link BuiltInSqlRewriters#allAvailable()}.</p>
 */
public enum BuiltInRewriteRule {
    /**
     * Injects or clamps LIMIT according to policy.
     */
    LIMIT_INJECTION,

    /**
     * Qualifies identifiers using schema/catalog resolution.
     */
    SCHEMA_QUALIFICATION,

    /**
     * Qualifies unqualified columns using visible table sources and catalog resolution.
     */
    COLUMN_QUALIFICATION,

    /**
     * Normalizes identifier casing/quoting for stable rendering.
     */
    IDENTIFIER_NORMALIZATION,

    /**
     * Canonicalizes query structure for deterministic fingerprints and output.
     */
    CANONICALIZATION
}
