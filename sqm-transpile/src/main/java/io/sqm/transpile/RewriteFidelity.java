package io.sqm.transpile;

/**
 * Describes how closely a transpilation rewrite preserves source semantics.
 */
public enum RewriteFidelity {
    /**
     * The rewrite is intended to preserve semantics exactly.
     */
    EXACT,
    /**
     * The rewrite is executable but may differ semantically in edge cases.
     */
    APPROXIMATE,
    /**
     * No safe rewrite exists for the construct.
     */
    UNSUPPORTED
}
