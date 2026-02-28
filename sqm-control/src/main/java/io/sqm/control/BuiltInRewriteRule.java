package io.sqm.control;

/**
 * Built-in AST rewrite rules planned for SQL middleware policy enforcement.
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
     * Injects tenant predicates into top-level table sources according to configured tenant table policies.
     */
    TENANT_PREDICATE,

    /**
     * Normalizes identifier casing/quoting for stable rendering.
     */
    IDENTIFIER_NORMALIZATION,

    /**
     * Canonicalizes query structure for deterministic fingerprints and output.
     */
    CANONICALIZATION
}
